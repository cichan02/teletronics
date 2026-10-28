package by.piskunou.teletronics.core.domain;

import java.time.Instant;

public record IssuedDownloadToken(
        String token,
        Instant expiresAt
) {
}
