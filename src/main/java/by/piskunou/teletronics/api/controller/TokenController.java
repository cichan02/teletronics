package by.piskunou.teletronics.api.controller;

import by.piskunou.teletronics.api.mapper.TokenMapper;
import by.piskunou.teletronics.api.to.response.DownloadLinkResponse;
import by.piskunou.teletronics.api.utils.Headers;
import by.piskunou.teletronics.core.domain.IssuedDownloadToken;
import by.piskunou.teletronics.core.service.TokenService;
import by.piskunou.teletronics.core.utils.FileStorage;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TokenController {
    public static final String DOWNLOAD_LINK_PATH = "/download";
    public static final String DOWNLOAD_TOKEN_PARAM = "token";

    private final TokenService tokenService;
    private final FileStorage storage;

    private final TokenMapper mapper;

    @SneakyThrows
    @GetMapping(path = "/generate-link/{fileId}")
    public ResponseEntity<DownloadLinkResponse> issueDownloadLink(
            @Schema(example = "9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660") @RequestHeader(name = Headers.X_USER_ID) UUID userId,
            @PathVariable UUID fileId
    ) {
        IssuedDownloadToken issued = tokenService.issueDownloadToken(fileId, userId);
        return ResponseEntity.ok(mapper.toResponse(issued));
    }

    @SneakyThrows
    @GetMapping(path = DOWNLOAD_LINK_PATH)
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(DOWNLOAD_TOKEN_PARAM) String token
    ) {
        FileMetadata meta = tokenService.findByDownloadToken(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(meta.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString()
                )
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .contentLength(meta.getFileSize())
                .body(new FileSystemResource(storage.resolve(meta)));
    }
}
