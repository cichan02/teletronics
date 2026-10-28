package by.piskunou.teletronics.service;

import by.piskunou.teletronics.config.property.TokenDownloadProperties;
import by.piskunou.teletronics.core.domain.IssuedDownloadToken;
import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.domain.exception.impl.FileNotFoundException;
import by.piskunou.teletronics.core.domain.exception.impl.InvalidDownloadTokenException;
import by.piskunou.teletronics.core.service.impl.TokenServiceImpl;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Duration TTL = Duration.ofMinutes(15);

    @Mock
    private FileMetadataRepository repository;

    private TokenServiceImpl tokenService;

    private final UUID fileId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(new TokenDownloadProperties(SECRET, TTL), repository);
    }

    @Test
    void issueDownloadToken_ForOwnPrivateFile_ReturnsSignedToken() throws Exception {
        FileMetadata file = file(Visibility.PRIVATE, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file));

        Instant before = Instant.now().plus(TTL).minusSeconds(5);
        IssuedDownloadToken issued = tokenService.issueDownloadToken(fileId, ownerId);
        Instant after = Instant.now().plus(TTL).plusSeconds(5);

        assertThat(issued.token()).isNotBlank().contains(".");
        assertThat(issued.expiresAt()).isBetween(before, after);
    }

    @Test
    void issueDownloadToken_ForPublicFileOfAnotherUser_ReturnsSignedToken() throws Exception {
        FileMetadata file = file(Visibility.PUBLIC, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file));

        IssuedDownloadToken issued = tokenService.issueDownloadToken(fileId, UUID.randomUUID());

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void issueDownloadToken_ForPrivateFileOfAnotherUser_ThrowsFileNotFound() {
        FileMetadata file = file(Visibility.PRIVATE, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> tokenService.issueDownloadToken(fileId, UUID.randomUUID()))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void issueDownloadToken_ForMissingFile_ThrowsFileNotFound() {
        when(repository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.issueDownloadToken(fileId, ownerId))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void findByDownloadToken_ForValidToken_ReturnsFile() throws Exception {
        FileMetadata file = file(Visibility.PRIVATE, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file));

        IssuedDownloadToken issued = tokenService.issueDownloadToken(fileId, ownerId);

        assertThat(tokenService.findByDownloadToken(issued.token())).isSameAs(file);
    }

    @Test
    void findByDownloadToken_ForTamperedToken_ThrowsInvalidToken() throws Exception {
        FileMetadata file = file(Visibility.PRIVATE, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file));

        IssuedDownloadToken issued = tokenService.issueDownloadToken(fileId, ownerId);
        String token = issued.token();
        int separator = token.indexOf('.');
        String signature = token.substring(separator + 1);
        char replacement = signature.charAt(0) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, separator + 1) + replacement + signature.substring(1);

        assertThatThrownBy(() -> tokenService.findByDownloadToken(tampered))
                .isInstanceOf(InvalidDownloadTokenException.class)
                .hasMessageContaining("Invalid download token");
    }

    @Test
    void findByDownloadToken_ForMalformedToken_ThrowsInvalidToken() {
        assertThatThrownBy(() -> tokenService.findByDownloadToken("not-a-valid-token"))
                .isInstanceOf(InvalidDownloadTokenException.class)
                .hasMessageContaining("Invalid download token");
    }

    @Test
    void findByDownloadToken_ForExpiredToken_ThrowsInvalidToken() throws Exception {
        FileMetadata file = file(Visibility.PRIVATE, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file));

        TokenServiceImpl expiredIssuer = new TokenServiceImpl(
                new TokenDownloadProperties(SECRET, Duration.ofMinutes(-1)), repository);
        IssuedDownloadToken expired = expiredIssuer.issueDownloadToken(fileId, ownerId);

        assertThatThrownBy(() -> tokenService.findByDownloadToken(expired.token()))
                .isInstanceOf(InvalidDownloadTokenException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    void findByDownloadToken_WhenFileWasDeleted_ThrowsFileNotFound() throws Exception {
        FileMetadata file = file(Visibility.PRIVATE, ownerId);
        when(repository.findById(fileId)).thenReturn(Optional.of(file), Optional.empty());

        IssuedDownloadToken issued = tokenService.issueDownloadToken(fileId, ownerId);

        assertThatThrownBy(() -> tokenService.findByDownloadToken(issued.token()))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    private FileMetadata file(Visibility visibility, UUID userId) {
        return FileMetadata.builder()
                .id(fileId)
                .basename("document")
                .fileExtension("txt")
                .visibility(visibility)
                .userId(userId)
                .contentType("text/plain")
                .fileSize(10L)
                .fileHash("hash")
                .tags(Set.of())
                .uploadDate(Instant.now())
                .build();
    }
}
