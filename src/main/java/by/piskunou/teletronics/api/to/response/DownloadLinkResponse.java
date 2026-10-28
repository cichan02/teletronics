package by.piskunou.teletronics.api.to.response;

import java.net.URI;
import java.time.Instant;

public record DownloadLinkResponse(
        URI link,
        Instant expiresAt
) {
}
