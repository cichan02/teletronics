package by.piskunou.teletronics.core.domain;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Document(collection = "file_metadata")
@CompoundIndex(name = "user_hash_idx", def = "{'userId': 1, 'fileHash': 1}")
@CompoundIndex(name = "user_filename_idx", def = "{'userId': 1, 'filename': 1}")
@Getter
@Setter
@Builder
public final class FileMetadata {
    public static final String SENTINEL_BASENAME = "Replace me!";
    public static final String SENTINEL_CONTENT_TYPE = "Detect me!";

    @Id
    private final UUID id;

    @Indexed
    private String basename;

    private final Visibility visibility;

    @Indexed
    private final UUID userId;

    private String fileExtension;

    @Indexed
    private final String contentType;

    @Indexed
    private final Long fileSize;

    private final String fileHash;

    private final String storagePath;

    private final Set<String> tags;

    @Indexed
    private final Instant uploadDate;

    public boolean isSentinelBasename() {
        return SENTINEL_BASENAME.equals(this.basename);
    }

    public boolean isSentinelContentType() {
        return SENTINEL_CONTENT_TYPE.equals(this.contentType);
    }
}
