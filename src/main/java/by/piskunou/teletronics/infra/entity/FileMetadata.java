package by.piskunou.teletronics.infra.entity;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Document(collection = "file_metadata")
@CompoundIndex(name = "user_hash_idx", def = "{'userId': 1, 'fileHash': 1}")
@CompoundIndex(name = "user_filename_idx", def = "{'userId': 1, 'basename': 1, 'fileExtension': 1}")
@Getter
@Setter
@Builder
@FieldNameConstants
public final class FileMetadata {
    public static final String SENTINEL_BASENAME = "Replace me!";
    public static final String SENTINEL_CONTENT_TYPE = "Detect me!";

    @Id
    private final UUID id;

    @Indexed
    private String basename;

    private String fileExtension;

    private final Visibility visibility;

    @Indexed
    private final UUID userId;

    @Indexed
    private final String contentType;

    @Indexed
    private final Long fileSize;

    private final String fileHash;

    private final Set<String> tags;

    @Indexed
    private final Instant uploadDate;

    public boolean isSentinelBasename() {
        return SENTINEL_BASENAME.equals(this.basename);
    }

    public boolean isSentinelContentType() {
        return SENTINEL_CONTENT_TYPE.equals(this.contentType);
    }

    public boolean isPublic() {
        return Visibility.PUBLIC.equals(this.visibility);
    }

    public boolean isPrivate() {
        return Visibility.PRIVATE.equals(this.visibility);
    }

    public String filename() {
        return basename + "." + fileExtension;
    }

    public Path relativePath() {
        return Paths.get(
                userId.toString(),
                id + "." + fileExtension
        );
    }
}
