//package by.piskunou.teletronics.service;
//
//import by.piskunou.teletronics.api.dto.*;
//import by.piskunou.teletronics.core.domain.FileMetadata;
//import by.piskunou.teletronics.core.domain.Tag;
//import by.piskunou.teletronics.core.domain.User;
//import by.piskunou.teletronics.core.domain.enumeration.Visibility;
//import by.piskunou.teletronics.core.service.FileStorageService;
//import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
//import by.piskunou.teletronics.infra.repository.TagRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.api.io.TempDir;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.mock.web.MockHttpServletRequest;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.time.Instant;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class FileStorageServiceTest {
//
//    @Mock
//    private FileMetadataRepository fileMetadataRepository;
//
//    @Mock
//    private TagRepository tagRepository;
//
//    @InjectMocks
//    private FileStorageService fileStorageService;
//
//    @TempDir
//    Path tempDir;
//
//    private User testUser;
//    private FileMetadata testFileMetadata;
//
//    @BeforeEach
//    void setUp() {
//        ReflectionTestUtils.setField(fileStorageService, "storageLocation", tempDir.toString());
//
//        // Setup mock request context for ServletUriComponentsBuilder
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.setScheme("http");
//        request.setServerName("localhost");
//        request.setServerPort(8080);
//        request.setContextPath("");
//        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
//        RequestContextHolder.setRequestAttributes(attrs);
//
//        testUser = User.builder()
//                .id("user123")
//                .username("testuser")
//                .build();
//
//        testFileMetadata = FileMetadata.builder()
//                .id("file123")
//                .filename("test.txt")
//                .originalFilename("test.txt")
//                .storagePath(tempDir.resolve("test.txt").toString())
//                .fileSize(1024L)
//                .contentType("text/plain")
//                .fileHash("abc123hash")
//                .downloadToken("token123")
//                .visibility(Visibility.PUBLIC)
//                .userId("user123")
//                .uploadDate(Instant.now())
//                .tags(Set.of("tag1", "tag2"))
//                .build();
//    }
//
//    @Test
//    void uploadFile_Success() throws IOException {
//        // Arrange
//        byte[] content = "test content".getBytes();
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                content
//        );
//
//        FileUploadRequest request = FileUploadRequest.builder()
//                .filename("custom-name.txt")
//                .visibility(Visibility.PUBLIC)
//                .tags(Set.of("tag1", "tag2"))
//                .contentType("text/plain")
//                .build();
//
//        when(fileMetadataRepository.existsByUserIdAndFileHash(anyString(), anyString())).thenReturn(false);
//        when(fileMetadataRepository.existsByUserIdAndFilename(anyString(), anyString())).thenReturn(false);
//        when(tagRepository.findByNameLower(anyString())).thenReturn(Optional.empty());
//        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // Act
//        FileUploadResponse response = fileStorageService.uploadFile(file, request, testUser);
//
//        // Assert
//        assertThat(response).isNotNull();
//        assertThat(response.getFilename()).isEqualTo("custom-name.txt");
//        assertThat(response.getContentType()).isEqualTo("text/plain");
//        assertThat(response.getFileSize()).isEqualTo(content.length);
//        assertThat(response.getDownloadLink()).contains("/api/files/download/");
//
//        verify(fileMetadataRepository).save(any(FileMetadata.class));
//        verify(tagRepository, times(2)).save(any(Tag.class));
//    }
//
//    @Test
//    void uploadFile_EmptyFile_ThrowsException() {
//        // Arrange
//        MockMultipartFile emptyFile = new MockMultipartFile(
//                "file",
//                "empty.txt",
//                "text/plain",
//                new byte[0]
//        );
//
//        FileUploadRequest request = FileUploadRequest.builder()
//                .visibility(Visibility.PUBLIC)
//                .build();
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.uploadFile(emptyFile, request, testUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("Cannot upload empty file");
//
//        verify(fileMetadataRepository, never()).save(any());
//    }
//
//    @Test
//    void uploadFile_DuplicateContent_ThrowsException() {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "test content".getBytes()
//        );
//
//        FileUploadRequest request = FileUploadRequest.builder()
//                .visibility(Visibility.PUBLIC)
//                .build();
//
//        when(fileMetadataRepository.existsByUserIdAndFileHash(anyString(), anyString())).thenReturn(true);
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.uploadFile(file, request, testUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("File with identical content already exists");
//
//        verify(fileMetadataRepository, never()).save(any());
//    }
//
//    @Test
//    void uploadFile_DuplicateFilename_ThrowsException() {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "test content".getBytes()
//        );
//
//        FileUploadRequest request = FileUploadRequest.builder()
//                .filename("existing-file.txt")
//                .visibility(Visibility.PUBLIC)
//                .build();
//
//        when(fileMetadataRepository.existsByUserIdAndFileHash(anyString(), anyString())).thenReturn(false);
//        when(fileMetadataRepository.existsByUserIdAndFilename(testUser.getId(), "existing-file.txt")).thenReturn(true);
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.uploadFile(file, request, testUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("File with name 'existing-file.txt' already exists");
//
//        verify(fileMetadataRepository, never()).save(any());
//    }
//
//    @Test
//    void uploadFile_UsesOriginalFilenameWhenCustomNotProvided() throws IOException {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "original.txt",
//                "text/plain",
//                "test content".getBytes()
//        );
//
//        FileUploadRequest request = FileUploadRequest.builder()
//                .visibility(Visibility.PUBLIC)
//                .build();
//
//        when(fileMetadataRepository.existsByUserIdAndFileHash(anyString(), anyString())).thenReturn(false);
//        when(fileMetadataRepository.existsByUserIdAndFilename(anyString(), anyString())).thenReturn(false);
//        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // Act
//        FileUploadResponse response = fileStorageService.uploadFile(file, request, testUser);
//
//        // Assert
//        assertThat(response.getFilename()).isEqualTo("original.txt");
//    }
//
//    @Test
//    void uploadFile_WithTags_LimitsToFive() throws IOException {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "test content".getBytes()
//        );
//
//        Set<String> manyTags = Set.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7");
//        FileUploadRequest request = FileUploadRequest.builder()
//                .visibility(Visibility.PUBLIC)
//                .tags(manyTags)
//                .build();
//
//        when(fileMetadataRepository.existsByUserIdAndFileHash(anyString(), anyString())).thenReturn(false);
//        when(fileMetadataRepository.existsByUserIdAndFilename(anyString(), anyString())).thenReturn(false);
//        when(tagRepository.findByNameLower(anyString())).thenReturn(Optional.empty());
//        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // Act
//        fileStorageService.uploadFile(file, request, testUser);
//
//        // Assert
//        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
//        verify(fileMetadataRepository).save(captor.capture());
//        assertThat(captor.getValue().getTags()).hasSizeLessThanOrEqualTo(5);
//    }
//
//    @Test
//    void updateFilename_Success() {
//        // Arrange
//        String fileId = "file123";
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("new-name.txt")
//                .build();
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));
//        when(fileMetadataRepository.existsByUserIdAndFilename(testUser.getId(), "new-name.txt")).thenReturn(false);
//        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // Act
//        fileStorageService.updateFilename(fileId, request, testUser);
//
//        // Assert
//        verify(fileMetadataRepository).save(argThat(metadata ->
//                metadata.getFilename().equals("new-name.txt")
//        ));
//    }
//
//    @Test
//    void updateFilename_FileNotFound_ThrowsException() {
//        // Arrange
//        String fileId = "nonexistent";
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("new-name.txt")
//                .build();
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.updateFilename(fileId, request, testUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("File not found");
//
//        verify(fileMetadataRepository, never()).save(any());
//    }
//
//    @Test
//    void updateFilename_NotOwner_ThrowsException() {
//        // Arrange
//        String fileId = "file123";
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("new-name.txt")
//                .build();
//
//        User differentUser = User.builder().id("different-user").build();
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.updateFilename(fileId, request, differentUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("You can only update your own files");
//
//        verify(fileMetadataRepository, never()).save(any());
//    }
//
//    @Test
//    void updateFilename_DuplicateName_ThrowsException() {
//        // Arrange
//        String fileId = "file123";
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("existing-file.txt")
//                .build();
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));
//        when(fileMetadataRepository.existsByUserIdAndFilename(testUser.getId(), "existing-file.txt")).thenReturn(true);
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.updateFilename(fileId, request, testUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("File with name 'existing-file.txt' already exists");
//
//        verify(fileMetadataRepository, never()).save(any());
//    }
//
//    @Test
//    void listPublicFiles_WithoutTag_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "uploadDate"));
//        List<FileMetadata> files = Arrays.asList(testFileMetadata);
//        Page<FileMetadata> page = new PageImpl<>(files, pageable, 1);
//
//        when(fileMetadataRepository.findByVisibility(Visibility.PUBLIC, pageable)).thenReturn(page);
//
//        // Act
//        PageResponse<FileMetadataDto> response = fileStorageService.listPublicFiles(null, pageable);
//
//        // Assert
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//        assertThat(response.getTotalElements()).isEqualTo(1);
//        assertThat(response.getPageNumber()).isEqualTo(0);
//        assertThat(response.getPageSize()).isEqualTo(20);
//
//        verify(fileMetadataRepository).findByVisibility(Visibility.PUBLIC, pageable);
//    }
//
//    @Test
//    void listPublicFiles_WithTag_Success() {
//        // Arrange
//        String tag = "tag1";
//        Pageable pageable = PageRequest.of(0, 20);
//        List<FileMetadata> files = Arrays.asList(testFileMetadata);
//        Page<FileMetadata> page = new PageImpl<>(files, pageable, 1);
//
//        when(fileMetadataRepository.findByVisibilityAndTagsContainingIgnoreCase(
//                Visibility.PUBLIC, tag, pageable)).thenReturn(page);
//
//        // Act
//        PageResponse<FileMetadataDto> response = fileStorageService.listPublicFiles(tag, pageable);
//
//        // Assert
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//        assertThat(response.getContent().get(0).getTags()).contains("tag1");
//
//        verify(fileMetadataRepository).findByVisibilityAndTagsContainingIgnoreCase(
//                Visibility.PUBLIC, tag, pageable);
//    }
//
//    @Test
//    void listUserFiles_WithoutTag_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 20);
//        List<FileMetadata> files = Arrays.asList(testFileMetadata);
//        Page<FileMetadata> page = new PageImpl<>(files, pageable, 1);
//
//        when(fileMetadataRepository.findByUserId(testUser.getId(), pageable)).thenReturn(page);
//
//        // Act
//        PageResponse<FileMetadataDto> response = fileStorageService.listUserFiles(testUser, null, pageable);
//
//        // Assert
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//        assertThat(response.getContent().get(0).getFilename()).isEqualTo("test.txt");
//
//        verify(fileMetadataRepository).findByUserId(testUser.getId(), pageable);
//    }
//
//    @Test
//    void listUserFiles_WithTag_Success() {
//        // Arrange
//        String tag = "tag2";
//        Pageable pageable = PageRequest.of(0, 20);
//        List<FileMetadata> files = Arrays.asList(testFileMetadata);
//        Page<FileMetadata> page = new PageImpl<>(files, pageable, 1);
//
//        when(fileMetadataRepository.findByUserIdAndTagsContainingIgnoreCase(
//                testUser.getId(), tag, pageable)).thenReturn(page);
//
//        // Act
//        PageResponse<FileMetadataDto> response = fileStorageService.listUserFiles(testUser, tag, pageable);
//
//        // Assert
//        assertThat(response).isNotNull();
//        assertThat(response.getContent()).hasSize(1);
//
//        verify(fileMetadataRepository).findByUserIdAndTagsContainingIgnoreCase(
//                testUser.getId(), tag, pageable);
//    }
//
//    @Test
//    void deleteFile_Success() throws IOException {
//        // Arrange
//        String fileId = "file123";
//        Path filePath = tempDir.resolve("test.txt");
//        Files.writeString(filePath, "test content");
//        testFileMetadata.setStoragePath(filePath.toString());
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));
//
//        // Act
//        fileStorageService.deleteFile(fileId, testUser);
//
//        // Assert
//        assertThat(Files.exists(filePath)).isFalse();
//        verify(fileMetadataRepository).delete(testFileMetadata);
//    }
//
//    @Test
//    void deleteFile_FileNotFound_ThrowsException() {
//        // Arrange
//        String fileId = "nonexistent";
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.deleteFile(fileId, testUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("File not found");
//
//        verify(fileMetadataRepository, never()).delete(any());
//    }
//
//    @Test
//    void deleteFile_NotOwner_ThrowsException() {
//        // Arrange
//        String fileId = "file123";
//        User differentUser = User.builder().id("different-user").build();
//
//        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.deleteFile(fileId, differentUser))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("You can only delete your own files");
//
//        verify(fileMetadataRepository, never()).delete(any());
//    }
//
//    @Test
//    void getFileByDownloadToken_Success() {
//        // Arrange
//        String token = "token123";
//        when(fileMetadataRepository.findByDownloadToken(token)).thenReturn(Optional.of(testFileMetadata));
//
//        // Act
//        FileMetadata result = fileStorageService.getFileByDownloadToken(token);
//
//        // Assert
//        assertThat(result).isNotNull();
//        assertThat(result.getDownloadToken()).isEqualTo(token);
//        assertThat(result.getFilename()).isEqualTo("test.txt");
//
//        verify(fileMetadataRepository).findByDownloadToken(token);
//    }
//
//    @Test
//    void getFileByDownloadToken_NotFound_ThrowsException() {
//        // Arrange
//        String token = "invalid-token";
//        when(fileMetadataRepository.findByDownloadToken(token)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThatThrownBy(() -> fileStorageService.getFileByDownloadToken(token))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("File not found");
//    }
//
//    @Test
//    void createPageable_DefaultValues() {
//        // Act
//        Pageable pageable = fileStorageService.createPageable(0, 20, null, null);
//
//        // Assert
//        assertThat(pageable.getPageNumber()).isEqualTo(0);
//        assertThat(pageable.getPageSize()).isEqualTo(20);
//        assertThat(pageable.getSort().getOrderFor("uploadDate")).isNotNull();
//        assertThat(pageable.getSort().getOrderFor("uploadDate").getDirection()).isEqualTo(Sort.Direction.ASC);
//    }
//
//    @Test
//    void createPageable_CustomSorting() {
//        // Act
//        Pageable pageable = fileStorageService.createPageable(1, 10, "filename", "desc");
//
//        // Assert
//        assertThat(pageable.getPageNumber()).isEqualTo(1);
//        assertThat(pageable.getPageSize()).isEqualTo(10);
//        assertThat(pageable.getSort().getOrderFor("filename")).isNotNull();
//        assertThat(pageable.getSort().getOrderFor("filename").getDirection()).isEqualTo(Sort.Direction.DESC);
//    }
//
//    @Test
//    void createPageable_AllSortFields() {
//        // Test all supported sort fields
//        String[] sortFields = {"filename", "uploadDate", "contentType", "fileSize"};
//
//        for (String sortField : sortFields) {
//            Pageable pageable = fileStorageService.createPageable(0, 20, sortField, "asc");
//            assertThat(pageable.getSort().getOrderFor(sortField)).isNotNull();
//        }
//    }
//
//    @Test
//    void createPageable_InvalidSortField_UsesDefault() {
//        // Act
//        Pageable pageable = fileStorageService.createPageable(0, 20, "invalid", "asc");
//
//        // Assert
//        assertThat(pageable.getSort().getOrderFor("uploadDate")).isNotNull();
//    }
//}
