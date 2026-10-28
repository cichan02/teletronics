package by.piskunou.teletronics.api.controller.handler;

import by.piskunou.teletronics.core.domain.exception.CannotDetectContentTypeException;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileContentTypeMismatchException;
import by.piskunou.teletronics.core.domain.exception.impl.FileNotFoundException;
import by.piskunou.teletronics.core.domain.exception.impl.FileWithContentAlreadyExistsException;
import by.piskunou.teletronics.core.domain.exception.impl.InvalidDownloadTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlerTest {

    private final ExceptionHandler handler = new ExceptionHandler();

    @Test
    void fileAlreadyExists_ReturnsConflict() {
        assertResponse(
                handler.handleDuplicateFile(new FileAlreadyExistsException("file.txt")),
                HttpStatus.CONFLICT,
                "File already exists"
        );
    }

    @Test
    void fileWithContentAlreadyExists_ReturnsConflict() {
        assertResponse(
                handler.handleDuplicateFile(new FileWithContentAlreadyExistsException("hash")),
                HttpStatus.CONFLICT,
                "A file with such content already exists"
        );
    }

    @Test
    void fileNotFound_ReturnsNotFound() {
        assertResponse(
                handler.handleFileNotFound(new FileNotFoundException(UUID.randomUUID())),
                HttpStatus.NOT_FOUND,
                "File not found"
        );
    }

    @Test
    void fileContentTypeMismatch_ReturnsBadRequest() {
        assertResponse(
                handler.handleFileContentTypeMismatch(new FileContentTypeMismatchException("text/plain", "application/pdf")),
                HttpStatus.BAD_REQUEST,
                "Mismatch between file Content-Type and specified Content-Type for file in request"
        );
    }

    @Test
    void invalidDownloadToken_ReturnsUnauthorized() {
        assertResponse(
                handler.handleInvalidDownloadToken(new InvalidDownloadTokenException()),
                HttpStatus.UNAUTHORIZED,
                "Invalid download token"
        );
    }

    @Test
    void expiredDownloadToken_ReturnsUnauthorized() {
        assertResponse(
                handler.handleInvalidDownloadToken(new InvalidDownloadTokenException("Token expired")),
                HttpStatus.UNAUTHORIZED,
                "Token expired"
        );
    }

    @Test
    void cannotDetectContentType_ReturnsInternalServerError() {
        assertResponse(
                handler.handleCannotDetectContentType(new CannotDetectContentTypeException()),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Cannot detect file content type"
        );
    }

    @Test
    void unmappedTeletronicsException_ReturnsBadRequest() {
        assertResponse(
                handler.handleTeletronicsException(new UnmappedDomainException()),
                HttpStatus.BAD_REQUEST,
                "Unmapped domain error"
        );
    }

    private void assertResponse(ResponseEntity<Map<String, String>> response,
                                HttpStatus expectedStatus,
                                String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).containsEntry("error", expectedMessage);
    }

    private static final class UnmappedDomainException extends TeletronicsException {
        private UnmappedDomainException() {
            super("Unmapped domain error");
        }
    }
}
