package by.piskunou.teletronics.api.to.request;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.Optional;

public record FileUploadRequest(
        Optional<String> filename,

        Optional<String> contentType,

        @NotNull
        Visibility visibility,

        @Size(max = 5)
        Collection<String> tags
) {

}
