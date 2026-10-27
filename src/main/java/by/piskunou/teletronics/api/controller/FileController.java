package by.piskunou.teletronics.api.controller;

import by.piskunou.teletronics.api.mapper.FileMetadataMapper;
import by.piskunou.teletronics.api.mapper.PageMapper;
import by.piskunou.teletronics.api.to.request.FileUploadRequest;
import by.piskunou.teletronics.api.to.request.FilenameUpdateRequest;
import by.piskunou.teletronics.api.to.response.FileMetadataResponse;
import by.piskunou.teletronics.api.to.response.collection.PageResponse;
import by.piskunou.teletronics.core.domain.FileMetadata;
import by.piskunou.teletronics.core.domain.User;
import by.piskunou.teletronics.core.domain.enumeration.SortBy;
import by.piskunou.teletronics.core.service.impl.FileServiceImpl;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileServiceImpl fileService;

    private final FileMetadataMapper fileMetadataMapper;
    private final PageMapper pageMapper;

    @GetMapping("/public")
    public ResponseEntity<PageResponse<FileMetadataResponse>> listPublicFiles(
            @RequestParam(required = false) Optional<String> tag,
            @RequestParam(defaultValue = "0")@Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = SortBy.DEFAULT) SortBy sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Page<FileMetadata> page = fileService.allPublicFiles(
                tag,
                pageMapper.toDomain(pageNumber, size, sortBy, direction)
        );
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent().stream().map(fileMetadataMapper::toResponse).toList(),
                pageMapper.toResponse(page)
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<FileMetadataResponse>> listMyFiles(
            @RequestParam(required = false) Optional<String> tag,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = SortBy.DEFAULT) SortBy sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @Parameter(hidden = true) @RequestAttribute User user
    ) {
        Page<FileMetadata> page = fileService.listUserFiles(
                user,
                tag,
                pageMapper.toDomain(pageNumber, size, sortBy, direction)
        );
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent().stream().map(fileMetadataMapper::toResponse).toList(),
                pageMapper.toResponse(page)
        ));
    }

    @SneakyThrows
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestAttribute User user
    ) throws IOException {
        FileMetadata meta = fileService.findById(id, user);

        InputStreamResource resource = new InputStreamResource(new FileInputStream(meta.getStoragePath()));

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getBasename() + "." + meta.getFileExtension()+ "\""
                )
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .contentLength(meta.getFileSize())
                .body(resource);
    }

    @SneakyThrows
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            //TODO: check if @NotNull works
            @RequestPart @Validated @NotNull MultipartFile file,
            @RequestPart @Validated FileUploadRequest uploadRequest,
            @Parameter(hidden = true) @RequestAttribute User user
    ) {
        FileMetadata domain = fileService.uploadFile(
                file,
                fileMetadataMapper.toDomain(uploadRequest),
                user
        );
        return ResponseEntity.ok(fileMetadataMapper.toResponse(domain));
    }

    @SneakyThrows
    @PatchMapping(path = "/{id}/filename")
    public ResponseEntity<Void> updateFilename(
            @PathVariable UUID id,
            @RequestBody @Validated FilenameUpdateRequest request,
            @Parameter(hidden = true) @RequestAttribute User user
    ) {
        fileService.updateFilename(id, request.filename(), user);
        return ResponseEntity.noContent().build();
    }

    @SneakyThrows
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestAttribute User user
    ) {
        fileService.deleteFile(id, user);
        return ResponseEntity.noContent().build();
    }
}
