package by.piskunou.teletronics.core.utils;

import by.piskunou.teletronics.config.property.StorageProperties;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public final class FileStorage {
    private static final String TMP_DIR = ".tmp";

    private final StorageProperties storageProps;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(basePath().resolve(TMP_DIR));
    }

    public Path basePath() {
        return Path.of(storageProps.location()).toAbsolutePath().normalize();
    }


    public Path resolve(FileMetadata meta) {
        return basePath().resolve(meta.relativePath());
    }

    public Path store(MultipartFile file, UUID userId, UUID fileId, String extension) throws IOException {
        Path userDir = Files.createDirectories(basePath().resolve(userId.toString()));
        Path target = userDir.resolve(fileId + "." + extension);
        file.transferTo(target.toFile());
        return target;
    }

    public void move(FileMetadata meta, String newExtension) throws IOException {
        Path source = resolve(meta);
        Files.move(source, source.resolveSibling(meta.getId() + "." + newExtension), StandardCopyOption.ATOMIC_MOVE);
    }

    public void delete(Path path) throws IOException {
        Files.deleteIfExists(path);
    }
}
