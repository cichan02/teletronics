package by.piskunou.teletronics.infra.repository.impl;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import by.piskunou.teletronics.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<String> findDistinctTagsByVisibility(Visibility visibility) {
        return distinctTags(Criteria.where(FileMetadata.Fields.visibility).is(visibility));
    }

    @Override
    public List<String> findDistinctTagsByUserId(UUID userId) {
        return distinctTags(Criteria.where(FileMetadata.Fields.userId).is(userId));
    }

    private List<String> distinctTags(Criteria criteria) {
        return mongoTemplate.query(FileMetadata.class)
                .distinct(FileMetadata.Fields.tags)
                .matching(Query.query(criteria))
                .as(String.class)
                .all()
                .stream()
                .sorted()
                .toList();
    }
}
