package by.piskunou.teletronics.api.controller;

import by.piskunou.teletronics.api.mapper.FileMetadataMapper;
import by.piskunou.teletronics.api.to.request.FilenameUpdateRequest;
import by.piskunou.teletronics.api.to.response.FileMetadataResponse;
import by.piskunou.teletronics.api.to.response.collection.PageResponse;
import by.piskunou.teletronics.core.domain.FileMetadata;
import by.piskunou.teletronics.core.domain.User;
import by.piskunou.teletronics.core.domain.enumeration.SortBy;
import by.piskunou.teletronics.core.service.FileService;
import by.piskunou.teletronics.api.to.request.FileUploadRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    private final FileMetadataMapper fileMetadataMapper;

    @SneakyThrows
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> uploadFile(
            //TODO: check if @NotNull works
            @RequestPart @Validated @NotNull MultipartFile file,
            @RequestPart @Validated FileUploadRequest uploadRequest,
            @Parameter(hidden = true) @RequestAttribute("user") User user
    ) {
        FileMetadata metadata = fileMetadataMapper.toDomain(uploadRequest);

        URI downloadLink = fileService.uploadFile(file, metadata, user);
        return ResponseEntity.created(downloadLink).build();
    }

    @SneakyThrows
    @PatchMapping(path = "/{id}/filename")
    public ResponseEntity<Void> updateFilename(
            @PathVariable UUID id,
            @RequestBody @Validated FilenameUpdateRequest request,
            @Parameter(hidden = true) @RequestAttribute("user") User user
    ) {
        fileService.updateFilename(id, request.filename(), user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public")
    public ResponseEntity<PageResponse<FileMetadataResponse>> listPublicFiles(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") SortBy sortBy,
            @RequestParam(defaultValue = "desc") Sort.Direction direction
    ) {
        Pageable pageable = fileService.createPageable(page, size, sortBy, direction);
        PageResponse<FileMetadataResponse> response = fileService.listPublicFiles(tag, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<FileMetadataResponse>> listMyFiles(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") SortBy sortBy,
            @RequestParam(defaultValue = "desc") Sort.Direction direction,
            @Parameter(hidden = true) @RequestAttribute("user") User user
    ) {
        Pageable pageable = fileService.createPageable(page, size, sortBy, direction);
        PageResponse<FileMetadataResponse> response = fileService.listUserFiles(user, tag, pageable);
        return ResponseEntity.ok(response);
    }

    @SneakyThrows
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestAttribute("user") User user
    ) {
        fileService.deleteFile(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID id
    ) throws IOException {
        FileMetadata metadata = fileService.getFileByDownloadToken(token);

        Path filePath = Paths.get(metadata.getStoragePath());
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(filePath.toFile()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getBasename() + "\"")
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .contentLength(metadata.getFileSize())
                .body(resource);
    }
}
