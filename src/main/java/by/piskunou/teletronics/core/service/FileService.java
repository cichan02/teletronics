package by.piskunou.teletronics.core.service;

import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public interface FileService {
    Page<FileMetadata> allPublicFiles(Optional<String> tag, Pageable pageable);

    Page<FileMetadata> listUserFiles(UUID userId, Optional<String> tag, Pageable pageable);

    FileMetadata uploadFile(MultipartFile file, FileMetadata meta, UUID userId) throws IOException, TeletronicsException;

    void updateFilename(UUID id, String newFilename, UUID userId) throws IOException, TeletronicsException;

    void deleteFile(UUID id, UUID userId) throws IOException, TeletronicsException;
}
