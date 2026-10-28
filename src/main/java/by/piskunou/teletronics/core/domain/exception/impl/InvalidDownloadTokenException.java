package by.piskunou.teletronics.core.domain.exception.impl;

import by.piskunou.teletronics.core.domain.exception.TeletronicsException;

public final class InvalidDownloadTokenException extends TeletronicsException {
    private static final String MESSAGE = "Invalid download token";

    public InvalidDownloadTokenException() {
        super(MESSAGE);
    }

    public InvalidDownloadTokenException(String message) {
        super(message);
    }
}
