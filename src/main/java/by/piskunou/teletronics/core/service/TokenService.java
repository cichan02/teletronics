package by.piskunou.teletronics.core.service;

import by.piskunou.teletronics.core.domain.IssuedDownloadToken;
import by.piskunou.teletronics.core.domain.exception.TeletronicsException;
import by.piskunou.teletronics.infra.entity.FileMetadata;

import java.util.UUID;

public interface TokenService {
    IssuedDownloadToken issueDownloadToken(UUID fileId, UUID userId) throws TeletronicsException;

    FileMetadata findByDownloadToken(String token) throws TeletronicsException;
}
