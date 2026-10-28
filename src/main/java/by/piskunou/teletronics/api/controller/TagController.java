package by.piskunou.teletronics.api.controller;

import by.piskunou.teletronics.api.to.response.collection.CollectionResponse;
import by.piskunou.teletronics.api.utils.Headers;
import by.piskunou.teletronics.core.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService service;

    @GetMapping(path = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionResponse<String>> listPublicTags() {
        return ResponseEntity.ok(new CollectionResponse<>(
                service.publicTags()
        ));
    }

    @GetMapping(path = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionResponse<String>> listMyTags(
            @RequestHeader(name = Headers.X_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(new CollectionResponse<>(
                service.userTags(userId)
        ));
    }
}
