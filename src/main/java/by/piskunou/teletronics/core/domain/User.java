package by.piskunou.teletronics.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "users")
public record User(
        @Id
        UUID id,

        @Indexed(unique = true)
        String username,

        Instant createdAt
) {
    public User(String username) {
        this(UUID.randomUUID(), username, Instant.now());
    }
}
