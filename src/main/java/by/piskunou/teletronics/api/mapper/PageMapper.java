package by.piskunou.teletronics.api.mapper;

import by.piskunou.teletronics.api.to.response.PaginationResponse;
import by.piskunou.teletronics.core.domain.enumeration.SortBy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class PageMapper {
    public Pageable toDomain(int page, int size, SortBy sortBy, Sort.Direction direction) {
        return PageRequest.of(page, size, Sort.by(direction, sortBy.sortField()));
    }

    public PaginationResponse toResponse(Page<?> page) {
        return new PaginationResponse(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
