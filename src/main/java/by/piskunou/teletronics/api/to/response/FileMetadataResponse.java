package by.piskunou.teletronics.api.to.response;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        String filename,
        Long fileSize,
        String contentType,
        Visibility visibility,
        Instant uploadDate,
        Set<String> tags,
        URI downloadLink
) {

}
