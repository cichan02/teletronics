package by.piskunou.teletronics.api.controller;

import by.piskunou.teletronics.api.mapper.FileMetadataMapper;
import by.piskunou.teletronics.api.mapper.PageMapper;
import by.piskunou.teletronics.api.to.request.FileUploadRequest;
import by.piskunou.teletronics.api.to.request.FilenameUpdateRequest;
import by.piskunou.teletronics.api.to.response.FileMetadataResponse;
import by.piskunou.teletronics.api.to.response.collection.PageResponse;
import by.piskunou.teletronics.api.utils.Headers;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.core.domain.enumeration.SortBy;
import by.piskunou.teletronics.core.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService service;

    private final FileMetadataMapper metadataMapper;
    private final PageMapper pageMapper;

    @GetMapping("/public")
    public ResponseEntity<PageResponse<FileMetadataResponse>> listPublicFiles(
            @RequestParam(required = false) Optional<String> tag,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = SortBy.DEFAULT) SortBy sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Page<FileMetadata> page = service.allPublicFiles(
                tag,
                pageMapper.toDomain(pageNumber, size, sortBy, direction)
        );
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent().stream().map(metadataMapper::toResponse).toList(),
                pageMapper.toResponse(page)
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<FileMetadataResponse>> listMyFiles(
            @Schema(example = "9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660") @RequestHeader(name = Headers.X_USER_ID) UUID userId,
            @RequestParam(required = false) Optional<String> tag,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = SortBy.DEFAULT) SortBy sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Page<FileMetadata> page = service.listUserFiles(
                userId,
                tag,
                pageMapper.toDomain(pageNumber, size, sortBy, direction)
        );
        return ResponseEntity.ok(new PageResponse<>(
                page.getContent().stream().map(metadataMapper::toResponse).toList(),
                pageMapper.toResponse(page)
        ));
    }

    @SneakyThrows
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = @Encoding(name = "uploadRequest", contentType = MediaType.APPLICATION_JSON_VALUE)
                    )
            )
    )
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @Schema(example = "9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660") @RequestHeader(name = Headers.X_USER_ID) UUID userId,
            @RequestPart @Validated @NotNull MultipartFile file,
            @RequestPart @Validated FileUploadRequest uploadRequest
    ) {
        FileMetadata domain = service.uploadFile(
                file,
                metadataMapper.toDomain(uploadRequest),
                userId
        );
        return ResponseEntity.ok(metadataMapper.toResponse(domain));
    }

    @SneakyThrows
    @PatchMapping(path = "/{id}/filename")
    public ResponseEntity<Void> updateFilename(
            @Schema(example = "9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660") @RequestHeader(name = Headers.X_USER_ID) UUID userId,
            @PathVariable UUID id,
            @RequestBody @Validated FilenameUpdateRequest request
    ) {
        service.updateFilename(id, request.filename(), userId);
        return ResponseEntity.noContent().build();
    }

    @SneakyThrows
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @Schema(example = "9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660") @RequestHeader(name = Headers.X_USER_ID) UUID userId,
            @PathVariable UUID id
    ) {
        service.deleteFile(id, userId);
        return ResponseEntity.noContent().build();
    }
}
