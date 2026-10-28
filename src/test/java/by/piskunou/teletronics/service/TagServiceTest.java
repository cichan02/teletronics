package by.piskunou.teletronics.service;

import by.piskunou.teletronics.core.domain.enumeration.Visibility;
import by.piskunou.teletronics.core.service.impl.TagServiceImpl;
import by.piskunou.teletronics.infra.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository repository;

    @InjectMocks
    private TagServiceImpl tagService;

    @Test
    void userTags_ReturnsDistinctTags() {
        UUID userId = UUID.randomUUID();
        when(repository.findDistinctTagsByUserId(userId)).thenReturn(List.of("tag2", "tag1"));

        List<String> tags = tagService.userTags(userId);

        assertThat(tags).containsExactlyInAnyOrder("tag1", "tag2");
        verify(repository).findDistinctTagsByUserId(userId);
    }

    @Test
    void publicTags_ReturnsDistinctTags() {
        when(repository.findDistinctTagsByVisibility(Visibility.PUBLIC)).thenReturn(List.of());

        List<String> tags = tagService.publicTags();

        assertThat(tags).isEmpty();
        verify(repository).findDistinctTagsByVisibility(Visibility.PUBLIC);
    }
}
