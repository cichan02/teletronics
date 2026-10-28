package by.piskunou.teletronics.core.service.impl;

import by.piskunou.teletronics.config.property.TokenDownloadProperties;
import by.piskunou.teletronics.core.domain.DownloadToken;
import by.piskunou.teletronics.core.domain.IssuedDownloadToken;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.core.domain.exception.impl.FileNotFoundException;
import by.piskunou.teletronics.core.domain.exception.impl.InvalidDownloadTokenException;
import by.piskunou.teletronics.core.service.TokenService;
import by.piskunou.teletronics.core.utils.FileUtils;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.infra.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {
    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SecretKeySpec key;
    private final Duration ttl;

    private final FileMetadataRepository repository;

    public TokenServiceImpl(TokenDownloadProperties downloadProps, FileMetadataRepository repository) {
        this.key = new SecretKeySpec(downloadProps.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.ttl = downloadProps.ttl();
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public IssuedDownloadToken issueDownloadToken(UUID fileId, UUID userId) throws TeletronicsException {
        //check if file belongs to user
        repository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        Instant expiresAt = Instant.now().plus(ttl);
        String payload = String.join("|",
                fileId.toString(),
                userId.toString(),
                Long.toString(expiresAt.getEpochSecond())
        );
        String body = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new IssuedDownloadToken(body + "." + ENCODER.encodeToString(sign(body)), expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public FileMetadata findByDownloadToken(String token) throws TeletronicsException {
        DownloadToken dt = verify(token);
        return repository.findById(dt.fileId())
                .filter(fm -> FileUtils.isVisibleForUser(fm, dt.ownerId()))
                .orElseThrow(() -> new FileNotFoundException(dt.fileId()));
    }

    private DownloadToken verify(String token) throws InvalidDownloadTokenException {
        try {
            int dot = token.indexOf('.');
            String body = token.substring(0, dot);
            if (!MessageDigest.isEqual(sign(body), DECODER.decode(token.substring(dot + 1)))) {
                throw new InvalidDownloadTokenException();
            }
            String[] p = new String(DECODER.decode(body), StandardCharsets.UTF_8).split("\\|");
            DownloadToken parsed = new DownloadToken(
                    UUID.fromString(p[0]),
                    UUID.fromString(p[1]),
                    Instant.ofEpochSecond(Long.parseLong(p[3]))
            );
            if (parsed.expiresAt().isBefore(Instant.now())) {
                throw new InvalidDownloadTokenException("Token expired");
            }
            return parsed;
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new InvalidDownloadTokenException();
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
