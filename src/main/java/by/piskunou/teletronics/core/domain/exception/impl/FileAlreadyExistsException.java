package by.piskunou.teletronics.core.domain.exception.impl;

import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import lombok.Getter;

@Getter
public final class FileAlreadyExistsException extends TeletronicsException {
    private static final String MESSAGE = "File already exists";

    private final String filename;

    public FileAlreadyExistsException(String filename) {
        super(MESSAGE);
        this.filename = filename;
    }
}
