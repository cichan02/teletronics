package by.piskunou.teletronics.infra.repository;

import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, UUID> {
    Optional<FileMetadata> findByIdAndUserId(UUID id, UUID userId);

    Page<FileMetadata> findByVisibility(Visibility visibility, Pageable pageable);

    @Query("{ 'visibility': ?0, 'tags': { $regex: ?1, $options: 'i' } }")
    Page<FileMetadata> findByVisibilityAndTagsContainingIgnoreCase(
            Visibility visibility,
            String tag,
            Pageable pageable
    );

    Page<FileMetadata> findByUserId(UUID userId, Pageable pageable);

    @Query("{ 'userId': ?0, 'tags': { $regex: ?1, $options: 'i' } }")
    Page<FileMetadata> findByUserIdAndTagsContainingIgnoreCase(
            UUID userId,
            String tag,
            Pageable pageable
    );

    boolean existsByUserIdAndFileHash(UUID userId, String fileHash);

    boolean existsByUserIdAndBasenameAndFileExtension(UUID userId, String basename, String fileExtension);
}
