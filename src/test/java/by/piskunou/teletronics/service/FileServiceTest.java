package by.piskunou.teletronics.service;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileContentTypeMismatchException;
import by.piskunou.teletronics.core.domain.exception.impl.FileWithContentAlreadyExistsException;
import by.piskunou.teletronics.core.service.impl.FileServiceImpl;
import by.piskunou.teletronics.core.utils.FileHasher;
import by.piskunou.teletronics.core.utils.FileStorage;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final String CONTENT = "test content";

    @Mock
    private FileMetadataRepository repository;

    @Mock
    private Tika tika;

    @Mock
    private FileStorage storage;

    @InjectMocks
    private FileServiceImpl fileService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void uploadFile_Success_WithCustomBasename() throws IOException, TeletronicsException {
        // Arrange
        MockMultipartFile file = multipartFile("original-name.txt", "text/plain");
        FileMetadata meta = metadata("custom-name", "txt", "text/plain", Set.of("tag1"));
        String fileHash = sha256Hex(CONTENT);
        Path storedPath = Path.of("/tmp/teletronics/" + userId + "/" + UUID.randomUUID() + ".txt");

        when(repository.existsByUserIdAndBasenameAndFileExtension(userId, "custom-name", "txt")).thenReturn(false);
        when(tika.detect(any(InputStream.class), any(Metadata.class))).thenReturn("text/plain");
        when(repository.existsByUserIdAndFileHash(userId, fileHash)).thenReturn(false);
        when(storage.store(any(MultipartFile.class), any(UUID.class), any(UUID.class), eq("txt"))).thenReturn(storedPath);
        when(repository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FileMetadata result = fileService.uploadFile(file, meta, userId);

        // Assert
        assertThat(result.getBasename()).isEqualTo("custom-name");
        assertThat(result.getFileExtension()).isEqualTo("txt");
        assertThat(result.filename()).isEqualTo("custom-name.txt");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        assertThat(result.getFileHash()).isEqualTo(fileHash);
        assertThat(result.getFileSize()).isEqualTo((long) CONTENT.getBytes(StandardCharsets.UTF_8).length);
        assertThat(result.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTags()).containsExactlyInAnyOrder("tag1");
        assertThat(result.getId()).isNotNull();

        verify(repository).save(any(FileMetadata.class));
        verify(storage).store(any(MultipartFile.class), eq(userId), any(UUID.class), eq("txt"));
    }

    @Test
    void uploadFile_Success_WhenBasenameAndContentTypeAreSentinels() throws IOException, TeletronicsException {
        // Arrange - sentinel basename/content type means values must be taken from the uploaded file itself
        MockMultipartFile file = multipartFile("test.txt", "text/plain");
        FileMetadata meta = FileMetadata.builder()
                .id(UUID.randomUUID())
                .basename(FileMetadata.SENTINEL_BASENAME)
                .fileExtension("txt")
                .visibility(Visibility.PUBLIC)
                .userId(userId)
                .contentType(FileMetadata.SENTINEL_CONTENT_TYPE)
                .fileSize((long) CONTENT.getBytes(StandardCharsets.UTF_8).length)
                .fileHash(sha256Hex(CONTENT))
                .tags(Set.of())
                .uploadDate(Instant.now())
                .build();
        String fileHash = sha256Hex(CONTENT);
        Path storedPath = Path.of("/tmp/teletronics/" + userId + "/" + UUID.randomUUID() + ".txt");

        when(repository.existsByUserIdAndBasenameAndFileExtension(userId, "test", "txt")).thenReturn(false);
        when(tika.detect(any(InputStream.class), any(Metadata.class))).thenReturn("text/plain");
        when(repository.existsByUserIdAndFileHash(userId, fileHash)).thenReturn(false);
        when(storage.store(any(MultipartFile.class), any(UUID.class), any(UUID.class), eq("txt"))).thenReturn(storedPath);
        when(repository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FileMetadata result = fileService.uploadFile(file, meta, userId);

        // Assert
        assertThat(result.getBasename()).isEqualTo("test");
        assertThat(result.getFileExtension()).isEqualTo("txt");
        assertThat(result.filename()).isEqualTo("test.txt");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        verify(storage).store(any(MultipartFile.class), any(UUID.class), any(UUID.class), eq("txt"));
    }

    @Test
    void uploadFile_MissingFilename_ThrowsIllegalArgumentException() throws IOException, TeletronicsException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", CONTENT.getBytes(StandardCharsets.UTF_8));
        FileMetadata meta = metadata("custom-name", "txt", "text/plain", Set.of());

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file, meta, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename must be provided");

        verify(repository, never()).save(any(FileMetadata.class));
        verify(storage, never()).store(any(MultipartFile.class), any(UUID.class), any(UUID.class), anyString());
    }

    @Test
    void uploadFile_DuplicateBasename_ThrowsException() throws IOException, TeletronicsException {
        // Arrange
        MockMultipartFile file = multipartFile("existing.txt", "text/plain");
        FileMetadata meta = metadata("existing", "txt", "text/plain", Set.of());

        when(repository.existsByUserIdAndBasenameAndFileExtension(userId, "existing", "txt")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file, meta, userId))
                .isInstanceOf(FileAlreadyExistsException.class)
                .hasMessageContaining("File already exists");

        verify(repository, never()).save(any(FileMetadata.class));
        verify(storage, never()).store(any(MultipartFile.class), any(UUID.class), any(UUID.class), anyString());
    }

    @Test
    void uploadFile_DuplicateContent_ThrowsException() throws IOException, TeletronicsException {
        // Arrange
        MockMultipartFile file = multipartFile("test.txt", "text/plain");
        FileMetadata meta = metadata("test", "txt", "text/plain", Set.of());

        when(repository.existsByUserIdAndBasenameAndFileExtension(userId, "test", "txt")).thenReturn(false);
        when(tika.detect(any(InputStream.class), any(Metadata.class))).thenReturn("text/plain");
        when(repository.existsByUserIdAndFileHash(userId, sha256Hex(CONTENT))).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file, meta, userId))
                .isInstanceOf(FileWithContentAlreadyExistsException.class);

        verify(repository, never()).save(any(FileMetadata.class));
        verify(storage, never()).store(any(MultipartFile.class), any(UUID.class), any(UUID.class), anyString());
    }

    @Test
    void uploadFile_ContentTypeMismatch_ThrowsException() throws IOException, TeletronicsException {
        // Arrange
        MockMultipartFile file = multipartFile("custom-name.txt", "text/plain");
        FileMetadata meta = metadata("custom-name", "txt", "application/pdf", Set.of());

        when(repository.existsByUserIdAndBasenameAndFileExtension(userId, "custom-name", "txt")).thenReturn(false);
        when(tika.detect(any(InputStream.class), any(Metadata.class))).thenReturn("text/plain");

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file, meta, userId))
                .isInstanceOf(FileContentTypeMismatchException.class)
                .hasMessageContaining("Mismatch");

        verify(repository, never()).save(any(FileMetadata.class));
        verify(storage, never()).store(any(MultipartFile.class), any(UUID.class), any(UUID.class), anyString());
    }

    @Test
    void uploadFile_SaveFailure_DeletesStoredFile() throws IOException, TeletronicsException {
        // Arrange
        MockMultipartFile file = multipartFile("custom-name.txt", "text/plain");
        FileMetadata meta = metadata("custom-name", "txt", "text/plain", Set.of("tag1"));
        String fileHash = sha256Hex(CONTENT);
        Path storedPath = Path.of("/tmp/teletronics/" + userId + "/" + UUID.randomUUID() + ".txt");

        when(repository.existsByUserIdAndBasenameAndFileExtension(userId, "custom-name", "txt")).thenReturn(false);
        when(tika.detect(any(InputStream.class), any(Metadata.class))).thenReturn("text/plain");
        when(repository.existsByUserIdAndFileHash(userId, fileHash)).thenReturn(false);
        when(storage.store(any(MultipartFile.class), any(UUID.class), any(UUID.class), eq("txt"))).thenReturn(storedPath);
        when(repository.save(any(FileMetadata.class))).thenThrow(new RuntimeException("Database failure"));

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file, meta, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database failure");

        // stored file must be cleaned up when persisting the metadata fails
        verify(storage).delete(storedPath);
    }

    private MockMultipartFile multipartFile(String originalFilename, String contentType) {
        return new MockMultipartFile("file", originalFilename, contentType, CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private FileMetadata metadata(String basename, String fileExtension, String contentType, Set<String> tags) {
        return FileMetadata.builder()
                .id(UUID.randomUUID())
                .basename(basename)
                .fileExtension(fileExtension)
                .visibility(Visibility.PUBLIC)
                .userId(userId)
                .contentType(contentType)
                .fileSize((long) CONTENT.getBytes(StandardCharsets.UTF_8).length)
                .fileHash(sha256Hex(CONTENT))
                .tags(tags)
                .uploadDate(Instant.now())
                .build();
    }

    private String sha256Hex(String content) {
        return FileHasher.calc(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
