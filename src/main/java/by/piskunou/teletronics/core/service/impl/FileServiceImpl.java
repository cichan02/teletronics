package by.piskunou.teletronics.core.service.impl;

import by.piskunou.teletronics.config.property.StorageProperties;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.domain.exception.CannotDetectContentTypeException;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileContentTypeMismatchException;
import by.piskunou.teletronics.core.domain.exception.impl.FileNotFoundException;
import by.piskunou.teletronics.core.domain.exception.impl.FileWithContentAlreadyExistsException;
import by.piskunou.teletronics.core.service.FileService;
import by.piskunou.teletronics.core.utils.FileUtils;
import by.piskunou.teletronics.core.utils.FileHasher;
import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileMetadataRepository repository;
    private final Tika tika;
    
    private final StorageProperties storageProps;;

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadata> allPublicFiles(Optional<String> tag, Pageable pageable) {
        if (isValidTag(tag)) {
            return repository.findByVisibilityAndTagsContainingIgnoreCase(
                    Visibility.PUBLIC,
                    tag.get(),
                    pageable
            );
        }
        return repository.findByVisibility(Visibility.PUBLIC, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadata> listUserFiles(UUID userId, Optional<String> tag, Pageable pageable) {
        if (isValidTag(tag)) {
            return repository.findByUserIdAndTagsContainingIgnoreCase(
                    userId,
                    tag.get(),
                    pageable
            );
        }

        return repository.findByUserId(userId, pageable);
    }

    private boolean isValidTag(Optional<String> tag) {
        return tag.isPresent() && !tag.get().isBlank();
    }

    @Override
    @Transactional
    public FileMetadata uploadFile(final MultipartFile file, final FileMetadata meta, final UUID userId) throws IOException, TeletronicsException {
        // fileHash section
        String fileHash = FileHasher.calc(file.getInputStream());
        if (repository.existsByUserIdAndFileHash(userId, fileHash)) {
            throw new FileWithContentAlreadyExistsException(fileHash);
        }

        // contentType section
        final String detectedContentType = detect(file);
        final String contentType = meta.isSentinelContentType() ? detectedContentType : meta.getContentType();
        if (!Objects.equals(detectedContentType, contentType)) {
            throw new FileContentTypeMismatchException(detectedContentType, contentType);
        }

        // filename section
        final String filename = meta.isSentinelBasename() ? file.getOriginalFilename() : meta.getBasename();

        // file extension section
        String fileExtension = FilenameUtils.getExtension(filename);
        if (!FileUtils.isValidExtension(fileExtension)) {
            fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
        }

        // basename section
        final String basename = FilenameUtils.getBaseName(filename);
        if (repository.existsByUserIdAndBasenameAndFileExtension(userId, basename, fileExtension)) {
            throw new FileAlreadyExistsException(basename + "." + fileExtension);
        }

        // store file on server
        final UUID fileId = UUID.randomUUID();
        Path uploadPath = Paths.get(storageProps.location(), userId.toString())
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(uploadPath);
        Path targetLocation = uploadPath.resolve(fileId + "." + fileExtension);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation);
        }

        // Create metadata
        FileMetadata forSave = FileMetadata.builder()
                .id(fileId)
                .basename(basename)
                .visibility(meta.getVisibility())
                .userId(userId)
                .fileExtension(fileExtension)
                .contentType(contentType)
                .fileSize(file.getSize())
                .fileHash(fileHash)
                .storagePath(targetLocation.toString())
                .tags(meta.getTags())
                .uploadDate(Instant.now())
                .build();
        repository.save(forSave);

        return forSave;
    }

    private String detect(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
            metadata.set(TikaCoreProperties.CONTENT_TYPE_HINT, file.getContentType());

            return tika.detect(in, metadata);
        } catch (IOException e) {
            throw new CannotDetectContentTypeException();
        }
    }

    @Override
    @Transactional
    public void updateFilename(UUID id, String newFilename, UUID userId) throws IOException, TeletronicsException {
        FileMetadata meta = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new FileNotFoundException(id));

        String newFileExtension = FilenameUtils.getExtension(newFilename);
        if (FileUtils.isValidExtension(newFileExtension) && !Objects.equals(newFileExtension, meta.getFileExtension())) {
            updateBasename(FilenameUtils.getBaseName(newFilename), newFileExtension, meta, userId);
            updateExtension(newFileExtension, meta, userId);
        } else {
            updateBasename(FilenameUtils.getBaseName(newFilename), meta.getFileExtension(), meta, userId);
        }

        repository.save(meta);
    }

    private void updateBasename(String newBasename, String fileExtension, FileMetadata meta, UUID userId) throws TeletronicsException {
        final String oldBasename = meta.getBasename();
        if (Objects.equals(newBasename, oldBasename)) {
            return;
        }

        if (repository.existsByUserIdAndBasenameAndFileExtension(userId, newBasename, fileExtension)) {
            throw new FileAlreadyExistsException(newBasename + "." + fileExtension);
        }

        meta.setBasename(newBasename);
    }

    private void updateExtension(String newExtension, FileMetadata meta, UUID userId) throws IOException {
        final String oldExtension = meta.getFileExtension();
        if (Objects.equals(newExtension, oldExtension)) {
            return;
        }

        meta.setFileExtension(newExtension);

        Path uploadPath = Paths.get(storageProps.location(), userId.toString())
                .toAbsolutePath()
                .normalize();
        Path source = uploadPath.resolve(meta.getId() + "." + oldExtension);
        Path target = uploadPath.resolve(meta.getId() + "." + newExtension);

        Files.move(source, target);
    }

    @Override
    @Transactional
    public void deleteFile(UUID id, UUID userId) throws IOException, TeletronicsException {
        FileMetadata meta = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new FileNotFoundException(id));

        Path filePath = Paths.get(storageProps.location(), userId.toString())
                .toAbsolutePath()
                .normalize()
                .resolve(meta.filename());
        Files.deleteIfExists(filePath);

        repository.deleteById(meta.getId());
    }
}
