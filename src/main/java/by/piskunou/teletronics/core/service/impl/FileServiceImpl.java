package by.piskunou.teletronics.core.service.impl;

import by.piskunou.teletronics.config.property.StorageProperties;
import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.domain.exception.CannotDetectContentTypeException;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileContentTypeMismatchException;
import by.piskunou.teletronics.core.domain.exception.impl.FileNotFoundException;
import by.piskunou.teletronics.core.domain.exception.impl.FileWithContentAlreadyExistsException;
import by.piskunou.teletronics.core.service.FileService;
import by.piskunou.teletronics.core.utils.FileHasher;
import by.piskunou.teletronics.core.utils.FileUtils;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
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
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileMetadataRepository repository;
    private final Tika tika;
    
    private final StorageProperties storageProps;

    @Override
    public Path resolve(FileMetadata fileMetadata) {
        return basePath()
                .resolve(fileMetadata.relativePath());
    }

    private Path basePath() {
        return Path.of(storageProps.location())
                .toAbsolutePath()
                .normalize();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadata> allPublicFiles(Optional<String> tag, Pageable pageable) {
        if (isValidTag(tag)) {
            return repository.findByVisibilityAndTagsContainingIgnoreCase(
                    Visibility.PUBLIC,
                    normalizeTag(tag.get()),
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
                    normalizeTag(tag.get()),
                    pageable
            );
        }

        return repository.findByUserId(userId, pageable);
    }

    private String normalizeTag(String tag) {
        return tag.trim().toLowerCase();
    }

    private boolean isValidTag(Optional<String> tag) {
        return tag.isPresent() && !tag.get().isBlank();
    }

    @Override
    @Transactional
    public FileMetadata uploadFile(final MultipartFile file, final FileMetadata meta, final UUID userId) throws IOException, TeletronicsException {
        // fileHash section
        final String fileHash;
        try(InputStream in = file.getInputStream()) {
            fileHash = FileHasher.calc(in);
        }

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
        if (filename == null || filename.isBlank()) {
            //TODO: write normal exception
            throw new IllegalArgumentException("Filename cannot be null");
        }

        // file extension section
        String fileExtension = FilenameUtils.getExtension(filename);
        if (!FileUtils.isValidExtension(fileExtension)) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                //TODO: write normal exception
                throw new IllegalArgumentException("Filename cannot be null");
            }
            fileExtension = FilenameUtils.getExtension(originalFilename);
        }

        // basename section
        final String basename = FilenameUtils.getBaseName(filename);
        if (repository.existsByUserIdAndBasenameAndFileExtension(userId, basename, fileExtension)) {
            throw new FileAlreadyExistsException(basename + "." + fileExtension);
        }

        // store file on server
        final UUID fileId = UUID.randomUUID();
        Path uploadPath = basePath().resolve(userId.toString());
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

        String newBasename = FilenameUtils.getBaseName(newFilename);
        String newFileExtension = FilenameUtils.getExtension(newFilename);
        if (FileUtils.isValidExtension(newFileExtension) && !Objects.equals(newFileExtension, meta.getFileExtension())) {
            updateBasename(newBasename, newFileExtension, meta, userId);
            updateExtension(newFileExtension, meta, userId);
        } else {
            updateBasename(newBasename, meta.getFileExtension(), meta, userId);
        }

        repository.save(meta);
    }

    private void updateBasename(String newBasename, String fileExtension, FileMetadata meta, UUID userId) throws TeletronicsException {
        if (repository.existsByUserIdAndBasenameAndFileExtension(userId, newBasename, fileExtension)) {
            throw new FileAlreadyExistsException(newBasename + "." + fileExtension);
        }

        if (Objects.equals(newBasename, meta.getBasename())) {
            return;
        }

        meta.setBasename(newBasename);
    }

    private void updateExtension(String newExtension, FileMetadata meta, UUID userId) throws IOException {
        Path source = resolve(meta);
        Path target = basePath()
                .resolve(
                        meta.getUserId().toString(),
                        meta.getId() + "." + newExtension
                );

        Files.move(source, target);

        meta.setFileExtension(newExtension);
    }

    @Override
    @Transactional
    public void deleteFile(UUID id, UUID userId) throws IOException, TeletronicsException {
        FileMetadata meta = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new FileNotFoundException(id));

        Files.deleteIfExists(resolve(meta));

        repository.deleteById(meta.getId());
    }
}
