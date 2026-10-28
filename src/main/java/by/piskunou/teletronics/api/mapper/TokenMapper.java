package by.piskunou.teletronics.api.mapper;

import by.piskunou.teletronics.api.controller.TokenController;
import by.piskunou.teletronics.api.to.response.DownloadLinkResponse;
import by.piskunou.teletronics.core.domain.IssuedDownloadToken;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class TokenMapper {
    public DownloadLinkResponse toResponse(IssuedDownloadToken domain) {
        URI link = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(TokenController.DOWNLOAD_LINK_PATH)
                .queryParam(TokenController.DOWNLOAD_TOKEN_PARAM, domain.token())
                .build()
                .toUri();
        return new DownloadLinkResponse(link, domain.expiresAt());
    }
}
