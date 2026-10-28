package by.piskunou.teletronics.core.service.impl;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.service.TagService;
import by.piskunou.teletronics.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<String> userTags(UUID userId) {
        return repository.findDistinctTagsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> publicTags() {
        return repository.findDistinctTagsByVisibility(Visibility.PUBLIC);
    }
}
