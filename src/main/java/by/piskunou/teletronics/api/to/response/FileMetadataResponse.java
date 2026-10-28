package by.piskunou.teletronics.api.to.response;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        String filename,
        Visibility visibility,
        String contentType,
        Long fileSize,
        Set<String> tags,
        Instant uploadDate
) {

}
