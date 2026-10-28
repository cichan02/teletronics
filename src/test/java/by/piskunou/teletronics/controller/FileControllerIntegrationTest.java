package by.piskunou.teletronics.controller;

import by.piskunou.teletronics.api.utils.Headers;
import by.piskunou.teletronics.utils.ZeroStreamingMultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration",
        "logging.level.org.mongodb.driver.cluster=WARN"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class FileControllerIntegrationTest {

    private static final String FILES_PATH = "/files";
    private static final long TWO_GIB = 2L * 1024 * 1024 * 1024;

    private static final Path STORAGE_DIR =
            Path.of("target", "integration-uploads-" + UUID.randomUUID()).toAbsolutePath();

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:9.0.4");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("file.storage.location", STORAGE_DIR::toString);
        registry.add("file.token-download.secret", () -> "integration-test-secret-32-bytes!!");
        registry.add("file.token-download.ttl", () -> "PT15M");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID userId = UUID.randomUUID();

    @AfterAll
    static void cleanUpStorage() {
        FileSystemUtils.deleteRecursively(STORAGE_DIR.toFile());
    }

    @Test
    void uploadFile_returnsMetadataAndStoresFile() throws Exception {
        String filename = "upload-" + UUID.randomUUID() + ".txt";
        byte[] content = "hello integration".getBytes(StandardCharsets.UTF_8);

        MvcResult result = mockMvc.perform(uploadRequest(filename, content))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.fileSize").value(content.length))
                .andExpect(jsonPath("$.tags[0]").value("integration"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UUID id = responseId(result);
        assertThat(Files.exists(STORAGE_DIR.resolve(userId.toString()).resolve(id + ".txt"))).isTrue();
    }

    @Test
    void listPublicFiles_returnsUploadedPublicFile() throws Exception {
        String filename = "public-" + UUID.randomUUID() + ".txt";
        UUID id = upload(filename, "public content");

        mockMvc.perform(get(FILES_PATH + "/public")
                        .param("pageNumber", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").exists());
    }

    @Test
    void listMyFiles_returnsOnlyCurrentUsersFiles() throws Exception {
        String filename = "mine-" + UUID.randomUUID() + ".txt";
        UUID id = upload(filename, "my content");

        mockMvc.perform(get(FILES_PATH + "/my")
                        .header(Headers.X_USER_ID, userId)
                        .param("pageNumber", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").exists());

        mockMvc.perform(get(FILES_PATH + "/my")
                        .header(Headers.X_USER_ID, UUID.randomUUID())
                        .param("pageNumber", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test
    void updateFilename_renamesFile() throws Exception {
        String filename = "before-" + UUID.randomUUID() + ".txt";
        UUID id = upload(filename, "rename content");
        String newName = "after-" + UUID.randomUUID() + ".txt";

        mockMvc.perform(patch(FILES_PATH + "/" + id + "/filename")
                        .header(Headers.X_USER_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"" + newName + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(FILES_PATH + "/my").header(Headers.X_USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')].filename").value(newName));
    }

    @Test
    void deleteFile_removesMetadataAndStoredFile() throws Exception {
        String filename = "delete-" + UUID.randomUUID() + ".txt";
        UUID id = upload(filename, "delete content");
        Path stored = STORAGE_DIR.resolve(userId.toString()).resolve(id + ".txt");
        assertThat(Files.exists(stored)).isTrue();

        mockMvc.perform(delete(FILES_PATH + "/" + id).header(Headers.X_USER_ID, userId))
                .andExpect(status().isNoContent());

        assertThat(Files.exists(stored)).isFalse();
        mockMvc.perform(get(FILES_PATH + "/my").header(Headers.X_USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test
    void uploadFile_parallelUploadsWithSameFilename_acceptsOnlyOne() throws Exception {
        int attempts = 6;
        String filename = "same-name.txt";
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            requests.add(uploadRequest(filename, ("content-" + i).getBytes(StandardCharsets.UTF_8)));
        }

        List<Integer> statuses = uploadConcurrently(requests).stream()
                .map(result -> result.getResponse().getStatus())
                .toList();

        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(attempts - 1);

        JsonNode page = listMyFiles();
        assertThat(page.get("items")).hasSize(1);
        assertThat(page.get("items").get(0).get("filename").asText()).isEqualTo(filename);
        assertThat(storedFiles()).hasSize(1);
    }

    @Test
    void uploadFile_parallelUploadsWithSameContent_acceptsOnlyOne() throws Exception {
        int attempts = 6;
        byte[] content = "identical content".getBytes(StandardCharsets.UTF_8);
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            requests.add(uploadRequest("same-content-" + i + ".txt", content));
        }

        List<Integer> statuses = uploadConcurrently(requests).stream()
                .map(result -> result.getResponse().getStatus())
                .toList();

        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(attempts - 1);

        JsonNode page = listMyFiles();
        assertThat(page.get("items")).hasSize(1);
        assertThat(page.get("pagination").get("totalElements").asLong()).isEqualTo(1);
        assertThat(storedFiles()).hasSize(1);
    }

    @Test
    void uploadFile_acceptsFileOfAtLeastTwoGigabytes() throws Exception {
        String filename = "huge.bin";
        MultipartFile file = new ZeroStreamingMultipartFile("file", filename, "application/octet-stream", TWO_GIB);

        MvcResult result = mockMvc.perform(uploadMultipart(file, "{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.fileSize").value(TWO_GIB))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UUID id = responseId(result);
        Path stored = STORAGE_DIR.resolve(userId.toString()).resolve(id + ".bin");
        assertThat(Files.size(stored)).isEqualTo(TWO_GIB);
    }

    private UUID upload(String filename, String content) throws Exception {
        MvcResult result = mockMvc.perform(uploadRequest(filename, content.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();
        return responseId(result);
    }

    private JsonNode listMyFiles() throws Exception {
        MvcResult result = mockMvc.perform(get(FILES_PATH + "/my").header(Headers.X_USER_ID, userId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private List<Path> storedFiles() throws IOException {
        Path userDir = STORAGE_DIR.resolve(userId.toString());
        if (!Files.exists(userDir)) {
            return List.of();
        }
        try (var paths = Files.list(userDir)) {
            return paths.toList();
        }
    }

    private UUID responseId(MvcResult result) throws IOException {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }

    private RequestBuilder uploadRequest(String filename, byte[] content) {
        return uploadMultipart(
                new MockMultipartFile("file", filename, "text/plain", content),
                "{\"visibility\":\"PUBLIC\",\"tags\":[\"integration\"]}"
        );
    }

    private RequestBuilder uploadMultipart(MultipartFile file, String metadataJson) {
        return servletContext -> {
            MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest(servletContext);
            request.setMethod("POST");
            request.setRequestURI(FILES_PATH + "/upload");
            request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
            request.addHeader(Headers.X_USER_ID, userId.toString());

            MockPart metadata = new MockPart("uploadRequest", metadataJson.getBytes(StandardCharsets.UTF_8));
            metadata.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            request.addPart(metadata);
            request.addFile(file);
            return request;
        };
    }

    private List<MvcResult> uploadConcurrently(List<RequestBuilder> requests) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        CyclicBarrier start = new CyclicBarrier(requests.size());
        try {
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (RequestBuilder request : requests) {
                futures.add(executor.submit(() -> {
                    start.await(15, TimeUnit.SECONDS);
                    return mockMvc.perform(request).andReturn();
                }));
            }
            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(120, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

}
