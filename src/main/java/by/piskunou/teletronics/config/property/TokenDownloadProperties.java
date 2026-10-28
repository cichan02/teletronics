package by.piskunou.teletronics.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "file.token-download")
public record TokenDownloadProperties(
        String secret,
        Duration ttl
) {
}
