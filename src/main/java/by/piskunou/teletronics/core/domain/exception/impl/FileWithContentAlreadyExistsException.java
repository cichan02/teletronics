package by.piskunou.teletronics.core.domain.exception.impl;

import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import lombok.Getter;

@Getter
public final class FileWithContentAlreadyExistsException extends TeletronicsException {
    private static final String MESSAGE = "A file with such content already exists";

    private final String fileHash;

    public FileWithContentAlreadyExistsException(String fileHash) {
        super(MESSAGE);
        this.fileHash = fileHash;
    }
}
