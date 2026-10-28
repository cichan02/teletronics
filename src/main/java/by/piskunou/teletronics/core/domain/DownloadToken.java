package by.piskunou.teletronics.core.domain;

import java.time.Instant;
import java.util.UUID;

public record DownloadToken(
        UUID fileId,
        UUID ownerId,
        Instant expiresAt
) {
}
