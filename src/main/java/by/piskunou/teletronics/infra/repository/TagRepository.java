package by.piskunou.teletronics.infra.repository;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;

import java.util.List;
import java.util.UUID;

public interface TagRepository {
    List<String> findDistinctTagsByVisibility(Visibility visibility);

    List<String> findDistinctTagsByUserId(UUID userId);

}
