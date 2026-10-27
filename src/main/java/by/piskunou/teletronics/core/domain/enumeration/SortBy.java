package by.piskunou.teletronics.core.domain.enumeration;

import by.piskunou.teletronics.core.domain.FileMetadata;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SortBy {
    FILENAME(FileMetadata.Fields.basename),
    UPLOAD_DATE(FileMetadata.Fields.uploadDate),
    TAG(FileMetadata.Fields.tags),
    CONTENT_TYPE(FileMetadata.Fields.contentType),
    FILE_SIZE(FileMetadata.Fields.fileSize);

    public static final String DEFAULT = "UPLOAD_DATE";

    private String sortField;

    public String sortField() {
        return this.sortField;
    }
}
