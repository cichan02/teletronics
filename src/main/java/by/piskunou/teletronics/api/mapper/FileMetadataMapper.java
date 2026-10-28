package by.piskunou.teletronics.api.mapper;

import by.piskunou.teletronics.api.to.request.FileUploadRequest;
import by.piskunou.teletronics.api.to.response.FileMetadataResponse;
import by.piskunou.teletronics.infra.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class FileMetadataMapper {
    public FileMetadata toDomain(FileUploadRequest request) {
        return FileMetadata.builder()
                .basename(request.filename()
                        .filter(fn -> !fn.isBlank())
                        .orElse(FileMetadata.SENTINEL_BASENAME)
                )
                .contentType(request
                        .contentType()
                        .map(MediaType::parseMediaType)
                        .map(mt -> mt.getType() + "/" + mt.getSubtype())
                        .orElse(FileMetadata.SENTINEL_CONTENT_TYPE)
                )
                .tags(Optional.ofNullable(request.tags())
                        .map(tags -> tags
                                .stream()
                                .filter(t -> t != null && !t.isBlank())
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet())
                        )
                        .orElse(Collections.emptySet())
                )
                .visibility(request.visibility())
                .build();
    }

    @Mapping(target = "filename", expression = "java(domain.filename())")
    public abstract FileMetadataResponse toResponse(FileMetadata domain);
}
