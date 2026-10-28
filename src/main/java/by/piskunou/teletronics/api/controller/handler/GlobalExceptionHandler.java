package by.piskunou.teletronics.api.controller.handler;

import by.piskunou.teletronics.core.domain.exception.CannotDetectContentTypeException;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileContentTypeMismatchException;
import by.piskunou.teletronics.core.domain.exception.impl.FileNotFoundException;
import by.piskunou.teletronics.core.domain.exception.impl.FileWithContentAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.InvalidDownloadTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({FileAlreadyExistsException.class, FileWithContentAlreadyExistsException.class})
    public ResponseEntity<Map<String, String>> handleDuplicateFile(Exception ex) {
        log.error("Duplicate file: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFileNotFound(FileNotFoundException ex) {
        log.error("File not found: {}", ex.getFileId());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FileContentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleFileContentTypeMismatch(FileContentTypeMismatchException ex) {
        log.error("Content type mismatch: detected={}, specified={}",
                ex.getDetectedContentType(), ex.getSpecifiedContentType());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidDownloadTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidDownloadToken(InvalidDownloadTokenException ex) {
        log.error("Invalid download token: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(CannotDetectContentTypeException.class)
    public ResponseEntity<Map<String, String>> handleCannotDetectContentType(CannotDetectContentTypeException ex) {
        log.error("Cannot detect content type: {}", ex.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(TeletronicsException.class)
    public ResponseEntity<Map<String, String>> handleTeletronicsException(TeletronicsException ex) {
        log.error("Domain exception: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "A file with the same name or content already exists");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error("File size limit exceeded: {}", ex.getMessage());
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "File size limit exceeded");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }
}
