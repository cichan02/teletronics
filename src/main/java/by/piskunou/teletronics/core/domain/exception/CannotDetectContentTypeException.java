package by.piskunou.teletronics.core.domain.exception;

public final class CannotDetectContentTypeException extends RuntimeException {
    private static final String MESSAGE = "Cannot detect file content type";

    public CannotDetectContentTypeException() {
        super(MESSAGE);
    }
}
