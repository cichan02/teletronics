package by.piskunou.teletronics.core.service;

import by.piskunou.teletronics.core.domain.FileMetadata;
import by.piskunou.teletronics.core.domain.User;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface FileService {
    FileMetadata findById(UUID id, User user) throws TeletronicsException;

    Page<FileMetadata> allPublicFiles(Optional<String> tag, Pageable pageable);

    Page<FileMetadata> listUserFiles(User user, Optional<String> tag, Pageable pageable);

    FileMetadata uploadFile(MultipartFile file, FileMetadata meta, User user) throws IOException, TeletronicsException;

    void updateFilename(UUID id, String newFilename, User user) throws IOException, TeletronicsException;

    void deleteFile(UUID id, User user) throws IOException, TeletronicsException;
}
