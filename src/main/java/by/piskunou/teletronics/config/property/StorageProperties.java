package by.piskunou.teletronics.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file.storage")
public record StorageProperties(
        String location
) {
}
