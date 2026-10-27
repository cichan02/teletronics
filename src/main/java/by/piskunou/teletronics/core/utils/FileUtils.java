package by.piskunou.teletronics.core.utils;

import by.piskunou.teletronics.core.domain.FileMetadata;
import by.piskunou.teletronics.core.domain.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*");

    public static boolean isValidExtension(String extension) {
        return extension != null && !extension.isBlank() && EXTENSION_PATTERN.matcher(extension).matches();
    }

    public static boolean isVisibleForUser(FileMetadata meta, User user) {
        return meta.isPublic() || meta.isPrivate() && meta.getUserId().equals(user.id());
    }
}
