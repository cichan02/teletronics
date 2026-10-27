package by.piskunou.teletronics.core.domain.exception.impl;

import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import lombok.Getter;

@Getter
public final class FileContentTypeMismatchException extends TeletronicsException {
    private static final String MESSAGE = "Mismatch between file Content-Type and specified Content-Type for file in request";

    private final String detectedContentType;
    private final String specifiedContentType;

    public FileContentTypeMismatchException(String detectedContentType, String specifiedContentType) {
        super(MESSAGE);
        this.detectedContentType = detectedContentType;
        this.specifiedContentType = specifiedContentType;
    }
}
