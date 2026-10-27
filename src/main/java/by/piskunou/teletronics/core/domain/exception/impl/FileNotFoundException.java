package by.piskunou.teletronics.core.domain.exception.impl;

import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class FileNotFoundException extends TeletronicsException {
    private static final String MESSAGE = "File not found";

    private final UUID fileId;

    public FileNotFoundException(UUID fileId) {
        super(MESSAGE);
        this.fileId = fileId;
    }
}
