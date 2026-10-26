//package by.piskunou.teletronics.controller;
//
//import by.piskunou.teletronics.TeletronicsApp;
//import by.piskunou.teletronics.api.to.request.FileUpdateRequest;
//import by.piskunou.teletronics.core.domain.FileMetadata;
//import by.piskunou.teletronics.core.domain.User;
//import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
//import by.piskunou.teletronics.infra.repository.UserRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.Matchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest(classes = TeletronicsApp.class)
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class FileControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private FileMetadataRepository fileMetadataRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    private User testUser;
//    private User anotherUser;
//
//    @BeforeEach
//    void setUp() {
//        // Clean up database
//        fileMetadataRepository.deleteAll();
//        userRepository.deleteAll();
//
//        // Create test users
//        testUser = User.builder()
//                .username("testuser")
//                .password("password")
//                .build();
//        testUser.onCreate();
//        testUser = userRepository.save(testUser);
//
//        anotherUser = User.builder()
//                .username("anotheruser")
//                .password("password")
//                .build();
//        anotherUser.onCreate();
//        anotherUser = userRepository.save(anotherUser);
//    }
//
//    @AfterEach
//    void tearDown() {
//        fileMetadataRepository.deleteAll();
//        userRepository.deleteAll();
//    }
//
//    @Test
//    void uploadFile_Success() throws Exception {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "Test file content".getBytes()
//        );
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file)
//                        .param("filename", "my-test-file.txt")
//                        .param("visibility", "PUBLIC")
//                        .param("tags", "tag1", "tag2")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.filename").value("my-test-file.txt"))
//                .andExpect(jsonPath("$.contentType").value("text/plain"))
//                .andExpect(jsonPath("$.fileSize").value(17))
//                .andExpect(jsonPath("$.downloadLink").exists())
//                .andExpect(jsonPath("$.uploadDate").exists());
//
//        // Verify file was saved to database
//        assertThat(fileMetadataRepository.count()).isEqualTo(1);
//        FileMetadata savedFile = fileMetadataRepository.findAll().get(0);
//        assertThat(savedFile.getFilename()).isEqualTo("my-test-file.txt");
//        assertThat(savedFile.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
//    }
//
//    @Test
//    void uploadFile_DuplicateContent_ReturnsBadRequest() throws Exception {
//        // Arrange
//        MockMultipartFile file1 = new MockMultipartFile(
//                "file",
//                "test1.txt",
//                "text/plain",
//                "Same content".getBytes()
//        );
//
//        MockMultipartFile file2 = new MockMultipartFile(
//                "file",
//                "test2.txt",
//                "text/plain",
//                "Same content".getBytes()
//        );
//
//        // Upload first file
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file1)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk());
//
//        // Act & Assert - Try to upload duplicate content
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file2)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void uploadFile_DuplicateFilename_ReturnsBadRequest() throws Exception {
//        // Arrange
//        MockMultipartFile file1 = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "First content".getBytes()
//        );
//
//        MockMultipartFile file2 = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "Different content".getBytes()
//        );
//
//        // Upload first file
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file1)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk());
//
//        // Act & Assert - Try to upload file with same name
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file2)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void uploadFile_EmptyFile_ReturnsBadRequest() throws Exception {
//        // Arrange
//        MockMultipartFile emptyFile = new MockMultipartFile(
//                "file",
//                "empty.txt",
//                "text/plain",
//                new byte[0]
//        );
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(emptyFile)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void uploadFile_WithoutCustomFilename_UsesOriginalFilename() throws Exception {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "original-name.txt",
//                "text/plain",
//                "Test content".getBytes()
//        );
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file)
//                        .param("visibility", "PRIVATE")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.filename").value("original-name.txt"));
//    }
//
//    @Test
//    void updateFilename_Success() throws Exception {
//        // Arrange
//        FileMetadata file = createTestFile(testUser, "old-name.txt", FileMetadata.Visibility.PUBLIC);
//
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("new-name.txt")
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(put("/api/files/{fileId}/filename", file.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request))
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk());
//
//        // Verify filename was updated
//        FileMetadata updated = fileMetadataRepository.findById(file.getId()).orElseThrow();
//        assertThat(updated.getFilename()).isEqualTo("new-name.txt");
//    }
//
//    @Test
//    void updateFilename_NotOwner_ReturnsBadRequest() throws Exception {
//        // Arrange
//        FileMetadata file = createTestFile(testUser, "test.txt", FileMetadata.Visibility.PUBLIC);
//
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("new-name.txt")
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(put("/api/files/{fileId}/filename", file.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request))
//                        .requestAttr("user", anotherUser))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void updateFilename_DuplicateName_ReturnsBadRequest() throws Exception {
//        // Arrange
//        createTestFile(testUser, "existing-name.txt", FileMetadata.Visibility.PUBLIC);
//        FileMetadata file2 = createTestFile(testUser, "file2.txt", FileMetadata.Visibility.PUBLIC);
//
//        FileUpdateRequest request = FileUpdateRequest.builder()
//                .filename("existing-name.txt")
//                .build();
//
//        // Act & Assert
//        mockMvc.perform(put("/api/files/{fileId}/filename", file2.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request))
//                        .requestAttr("user", testUser))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void listPublicFiles_Success() throws Exception {
//        // Arrange
//        createTestFile(testUser, "public1.txt", FileMetadata.Visibility.PUBLIC);
//        createTestFile(testUser, "public2.txt", FileMetadata.Visibility.PUBLIC);
//        createTestFile(testUser, "private1.txt", FileMetadata.Visibility.PRIVATE);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/files/public")
//                        .param("page", "0")
//                        .param("size", "20"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content", hasSize(2)))
//                .andExpect(jsonPath("$.totalElements").value(2))
//                .andExpect(jsonPath("$.pageNumber").value(0))
//                .andExpect(jsonPath("$.pageSize").value(20));
//    }
//
//    @Test
//    void listPublicFiles_WithTagFilter_Success() throws Exception {
//        // Arrange
//        createTestFile(testUser, "file1.txt", FileMetadata.Visibility.PUBLIC, Set.of("java", "spring"));
//        createTestFile(testUser, "file2.txt", FileMetadata.Visibility.PUBLIC, Set.of("python", "django"));
//        createTestFile(testUser, "file3.txt", FileMetadata.Visibility.PUBLIC, Set.of("java", "kotlin"));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/files/public")
//                        .param("tag", "java")
//                        .param("page", "0")
//                        .param("size", "20"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content", hasSize(2)))
//                .andExpect(jsonPath("$.content[*].tags", everyItem(hasItem("java"))));
//    }
//
//    @Test
//    void listPublicFiles_WithPagination_Success() throws Exception {
//        // Arrange - Create 25 files
//        for (int i = 0; i < 25; i++) {
//            createTestFile(testUser, "file" + i + ".txt", FileMetadata.Visibility.PUBLIC);
//        }
//
//        // Act & Assert - First page
//        mockMvc.perform(get("/api/files/public")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content", hasSize(10)))
//                .andExpect(jsonPath("$.totalElements").value(25))
//                .andExpect(jsonPath("$.totalPages").value(3))
//                .andExpect(jsonPath("$.last").value(false));
//
//        // Act & Assert - Last page
//        mockMvc.perform(get("/api/files/public")
//                        .param("page", "2")
//                        .param("size", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content", hasSize(5)))
//                .andExpect(jsonPath("$.last").value(true));
//    }
//
//    @Test
//    void listPublicFiles_WithSorting_Success() throws Exception {
//        // Arrange
//        createTestFile(testUser, "zebra.txt", FileMetadata.Visibility.PUBLIC);
//        Thread.sleep(10); // Ensure different timestamps
//        createTestFile(testUser, "alpha.txt", FileMetadata.Visibility.PUBLIC);
//
//        // Act & Assert - Sort by filename ascending
//        mockMvc.perform(get("/api/files/public")
//                        .param("sortBy", "filename")
//                        .param("sortDirection", "asc"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].filename").value("alpha.txt"))
//                .andExpect(jsonPath("$.content[1].filename").value("zebra.txt"));
//
//        // Act & Assert - Sort by filename descending
//        mockMvc.perform(get("/api/files/public")
//                        .param("sortBy", "filename")
//                        .param("sortDirection", "desc"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].filename").value("zebra.txt"))
//                .andExpect(jsonPath("$.content[1].filename").value("alpha.txt"));
//    }
//
//    @Test
//    void listMyFiles_Success() throws Exception {
//        // Arrange
//        createTestFile(testUser, "my-file1.txt", FileMetadata.Visibility.PUBLIC);
//        createTestFile(testUser, "my-file2.txt", FileMetadata.Visibility.PRIVATE);
//        createTestFile(anotherUser, "other-file.txt", FileMetadata.Visibility.PUBLIC);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/files/my-files")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content", hasSize(2)))
//                .andExpect(jsonPath("$.content[*].filename", hasItems("my-file1.txt", "my-file2.txt")));
//    }
//
//    @Test
//    void listMyFiles_WithTagFilter_Success() throws Exception {
//        // Arrange
//        createTestFile(testUser, "file1.txt", FileMetadata.Visibility.PUBLIC, Set.of("work"));
//        createTestFile(testUser, "file2.txt", FileMetadata.Visibility.PUBLIC, Set.of("personal"));
//        createTestFile(testUser, "file3.txt", FileMetadata.Visibility.PUBLIC, Set.of("work", "important"));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/files/my-files")
//                        .param("tag", "work")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content", hasSize(2)))
//                .andExpect(jsonPath("$.content[*].tags", everyItem(hasItem("work"))));
//    }
//
//    @Test
//    void deleteFile_Success() throws Exception {
//        // Arrange
//        FileMetadata file = createTestFile(testUser, "to-delete.txt", FileMetadata.Visibility.PUBLIC);
//        String fileId = file.getId();
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/files/{fileId}", fileId)
//                        .requestAttr("user", testUser))
//                .andExpect(status().isNoContent());
//
//        // Verify file was deleted
//        assertThat(fileMetadataRepository.existsById(fileId)).isFalse();
//    }
//
//    @Test
//    void deleteFile_NotOwner_ReturnsBadRequest() throws Exception {
//        // Arrange
//        FileMetadata file = createTestFile(testUser, "test.txt", FileMetadata.Visibility.PUBLIC);
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/files/{fileId}", file.getId())
//                        .requestAttr("user", anotherUser))
//                .andExpect(status().isBadRequest());
//
//        // Verify file was not deleted
//        assertThat(fileMetadataRepository.existsById(file.getId())).isTrue();
//    }
//
//    @Test
//    void deleteFile_NotFound_ReturnsBadRequest() throws Exception {
//        // Act & Assert
//        mockMvc.perform(delete("/api/files/{fileId}", "nonexistent-id")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void downloadFile_Success() throws Exception {
//        // Arrange
//        FileMetadata file = createTestFile(testUser, "download-test.txt", FileMetadata.Visibility.PUBLIC);
//
//        // Act & Assert
//        MvcResult result = mockMvc.perform(get("/api/files/download/{token}", file.getDownloadToken()))
//                .andExpect(status().isOk())
//                .andExpect(header().string("Content-Disposition", containsString("download-test.txt")))
//                .andExpect(content().contentType(MediaType.parseMediaType(file.getContentType())))
//                .andReturn();
//
//        // Verify file content
//        String content = result.getResponse().getContentAsString();
//        assertThat(content).isEqualTo("Test file content");
//    }
//
//    @Test
//    void downloadFile_InvalidToken_ReturnsNotFound() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/api/files/download/{token}", "invalid-token"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void downloadFile_PhysicalFileNotFound_ReturnsNotFound() throws Exception {
//        // Arrange
//        FileMetadata file = createTestFile(testUser, "missing.txt", FileMetadata.Visibility.PUBLIC);
//        // Delete the physical file but keep metadata
//        Path filePath = Paths.get(file.getStoragePath());
//        Files.deleteIfExists(filePath);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/files/download/{token}", file.getDownloadToken()))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void uploadFile_TagManagement_Success() throws Exception {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "tagged-file.txt",
//                "text/plain",
//                "Content with tags".getBytes()
//        );
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file)
//                        .param("visibility", "PUBLIC")
//                        .param("tags", "tag1", "tag2", "tag3")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk());
//
//        // Verify tags were saved
//        FileMetadata savedFile = fileMetadataRepository.findAll().get(0);
//        assertThat(savedFile.getTags()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
//    }
//
//    @Test
//    void uploadFile_MultipleUsers_IsolatedFiles() throws Exception {
//        // Arrange
//        MockMultipartFile file1 = new MockMultipartFile(
//                "file",
//                "same-name.txt",
//                "text/plain",
//                "User 1 content".getBytes()
//        );
//
//        MockMultipartFile file2 = new MockMultipartFile(
//                "file",
//                "same-name.txt",
//                "text/plain",
//                "User 2 content".getBytes()
//        );
//
//        // Act - Upload same filename for different users
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file1)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", testUser))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(multipart("/api/files/upload")
//                        .file(file2)
//                        .param("visibility", "PUBLIC")
//                        .requestAttr("user", anotherUser))
//                .andExpect(status().isOk());
//
//        // Assert - Both files should exist
//        assertThat(fileMetadataRepository.count()).isEqualTo(2);
//        assertThat(fileMetadataRepository.existsByUserIdAndFilename(testUser.getId(), "same-name.txt")).isTrue();
//        assertThat(fileMetadataRepository.existsByUserIdAndFilename(anotherUser.getId(), "same-name.txt")).isTrue();
//    }
//
//    // Helper methods
//
//    private FileMetadata createTestFile(User user, String filename, FileMetadata.Visibility visibility) {
//        return createTestFile(user, filename, visibility, Set.of());
//    }
//
//    private FileMetadata createTestFile(User user, String filename, FileMetadata.Visibility visibility, Set<String> tags) {
//        FileMetadata metadata = FileMetadata.builder()
//                .filename(filename)
//                .originalFilename(filename)
//                .storagePath(createPhysicalFile(filename))
//                .fileSize(17L)
//                .contentType("text/plain")
//                .fileHash("hash-" + filename + "-" + user.getId())
//                .visibility(visibility)
//                .userId(user.getId())
//                .tags(tags)
//                .build();
//
//        metadata.onCreate();
//        return fileMetadataRepository.save(metadata);
//    }
//
//    private String createPhysicalFile(String filename) {
//        try {
//            Path uploadDir = Paths.get("./test-uploads");
//            Files.createDirectories(uploadDir);
//            Path filePath = uploadDir.resolve(System.currentTimeMillis() + "_" + filename);
//            Files.writeString(filePath, "Test file content");
//            return filePath.toAbsolutePath().toString();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to create test file", e);
//        }
//    }
//}
