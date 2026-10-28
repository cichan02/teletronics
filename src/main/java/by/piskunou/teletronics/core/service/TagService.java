package by.piskunou.teletronics.core.service;

import java.util.List;
import java.util.UUID;

public interface TagService {
    List<String> userTags(UUID userId);

    List<String> publicTags();
}
