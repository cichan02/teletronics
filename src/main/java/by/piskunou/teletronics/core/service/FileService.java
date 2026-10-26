package by.piskunou.teletronics.core.service;

import by.piskunou.teletronics.api.to.response.FileMetadataResponse;
import by.piskunou.teletronics.api.to.response.collection.PageResponse;
import by.piskunou.teletronics.core.domain.FileMetadata;
import by.piskunou.teletronics.core.domain.User;
import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.utils.FileHasher;
import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final Tika tika;

    //TODO: transform into properties
    @Value("${file.storage.location}")
    private String storageLocation;

    @Transactional
    public URI uploadFile(final MultipartFile file, final FileMetadata meta, final User user) throws IOException {
        // fileHash section
        String fileHash = FileHasher.calc(file.getInputStream());
        if (fileMetadataRepository.existsByUserIdAndFileHash(user.id(), fileHash)) {
            throw new FileAlreadyExistsException(null, fileHash);
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
        if (!isValidFileExtension(fileExtension)) {
            fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
        }

        // basename section
        final String basename = FilenameUtils.getBaseName(filename);
        if (fileMetadataRepository.existsByUserIdAndBasename(user.id(), basename)) {
            throw new FileAlreadyExistsException(basename, null);
        }

        // store file on server
        final UUID fileId = UUID.randomUUID();
        Path uploadPath = Paths.get(storageLocation, user.id().toString())
                .toAbsolutePath()
                .normalize()
                .resolve(meta.getId() + "." + meta.getFileExtension());
        Files.createDirectories(uploadPath);
        Path targetLocation = uploadPath.resolve(fileId + "." + fileExtension);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation);
        }

        // Generate download link
        final URI downloadLink = generateDownloadLink(fileId);

        // Create metadata
        FileMetadata forSave = FileMetadata.builder()
                .id(UUID.randomUUID())
                .basename(basename)
                .visibility(meta.getVisibility())
                .userId(user.id())
                .fileExtension(fileExtension)
                .contentType(contentType)
                .fileSize(file.getSize())
                .fileHash(fileHash)
                .storagePath(targetLocation.toString())
                .tags(meta.getTags())
                .uploadDate(Instant.now())
                .build();
        fileMetadataRepository.save(forSave);

        return downloadLink;
    }

    @Transactional
    public void updateFilename(UUID id, String newFilename, User user) throws IOException {
        FileMetadata meta = fileMetadataRepository.findById(id.toString())
                .orElseThrow(FileNotFoundException::new);

        if (!meta.getUserId().equals(user.id())) {
            throw new NotAuthorizedException("You can only update your own files");
        }

        updateBasename(FilenameUtils.getBaseName(newFilename), meta, user);
        String newFileExtension = FilenameUtils.getExtension(newFilename);
        if (isValidFileExtension(newFileExtension) && !Objects.equals(newFileExtension, meta.getFileExtension())) {
            updateExtension(newFileExtension, meta, user);
        }

        fileMetadataRepository.save(meta);
    }

    private void updateBasename(String newBasename, FileMetadata meta, User user) {
        final String oldBasename = meta.getBasename();
        if (Objects.equals(newBasename, oldBasename)) {
            return;
        }

        if (fileMetadataRepository.existsByUserIdAndBasename(user.id(), newBasename)) {
            throw new FileAlreadyExistsException(newBasename, null);
        }

        meta.setBasename(newBasename);
    }

    private void updateExtension(String newExtension, FileMetadata meta, User user) throws IOException {
        final String oldExtension = meta.getFileExtension();
        if (Objects.equals(newExtension, oldExtension)) {
            return;
        }

        meta.setFileExtension(newExtension);

        Path uploadPath = Paths.get(storageLocation, user.id().toString())
                .toAbsolutePath()
                .normalize();
        Path source = uploadPath.resolve(meta.getId() + "." + oldExtension);
        Path target = uploadPath.resolve(meta.getId() + "." + newExtension);

        Files.move(source, target);
    }

    @Transactional(readOnly = true)
    public PageResponse<FileMetadataResponse> listPublicFiles(String tag, Pageable pageable) {
        Page<FileMetadata> page;

        if (tag != null && !tag.isBlank()) {
            page = fileMetadataRepository.findByVisibilityAndTagsContainingIgnoreCase(
                    Visibility.PUBLIC,
                    tag,
                    pageable
            );
        } else {
            page = fileMetadataRepository.findByVisibility(Visibility.PUBLIC, pageable);
        }

        return convertToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<FileMetadataResponse> listUserFiles(User user, String tag, Pageable pageable) {
        Page<FileMetadata> page;

        if (tag != null && !tag.isBlank()) {
            page = fileMetadataRepository.findByUserIdAndTagsContainingIgnoreCase(user.getId(), tag, pageable);
        } else {
            page = fileMetadataRepository.findByUserId(user.getId(), pageable);
        }

        return convertToPageResponse(page);
    }

    @Transactional
    public void deleteFile(UUID id, User user) throws IOException {
        FileMetadata meta = fileMetadataRepository.findById(id.toString())
                .orElseThrow(FileNotFoundException::new);

        if (!meta.getUserId().equals(user.id())) {
            throw new NotAuthorizedException("You can only update your own files");
        }

        Path filePath = Paths.get(storageLocation, user.id().toString())
                .toAbsolutePath()
                .normalize()
                .resolve(meta.getId() + "." + meta.getFileExtension());
        Files.deleteIfExists(filePath);

        fileMetadataRepository.delete(metadata);
    }

    private URI generateDownloadLink(UUID fileId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files")
                .path(fileId.toString())
                .path("/download")
                .build()
                .toUri();
    }

    private String detect(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
            metadata.set(TikaCoreProperties.CONTENT_TYPE_HINT, file.getContentType());

            return tika.detect(in, metadata);
        } catch (IOException e) {
            throw new CannotDetectContentTypeException("Cannot detect file content type", e);
        }
    }

    private PageResponse<FileMetadataResponse> convertToPageResponse(Page<FileMetadata> page) {
        return PageResponse.<FileMetadataResponse>builder()
                .items(page.getContent().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private FileMetadataResponse convertToDto(FileMetadata metadata) {
        return FileMetadataResponse.builder()
                .id(metadata.getId())
                .filename(metadata.getBasename())
                .fileSize(metadata.getFileSize())
                .contentType(metadata.getContentType())
                .visibility(metadata.getVisibility())
                .uploadDate(metadata.getUploadDate())
                .tags(metadata.getTags())
                .downloadLink(generateDownloadLink(metadata.getDownloadLink()))
                .build();
    }

    public Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String sortField = switch (sortBy != null ? sortBy.toLowerCase() : "uploadDate") {
            case "filename" -> "filename";
            case "uploaddate" -> "uploadDate";
            case "contenttype" -> "contentType";
            case "filesize" -> "fileSize";
            default -> "uploadDate";
        };

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    private boolean isValidFileExtension(String fileExtension) {
        return fileExtension != null && !fileExtension.isBlank();
    }
}
