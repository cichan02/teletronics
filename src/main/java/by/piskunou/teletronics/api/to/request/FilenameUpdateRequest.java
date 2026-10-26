package by.piskunou.teletronics.api.to.request;

import jakarta.validation.constraints.NotBlank;

public record FilenameUpdateRequest(
        @NotBlank
        String filename
) {

}
