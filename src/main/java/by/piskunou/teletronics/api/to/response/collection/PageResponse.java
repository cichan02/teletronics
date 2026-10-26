package by.piskunou.teletronics.api.to.response.collection;

import by.piskunou.teletronics.api.to.response.PaginationResponse;

import java.util.Collection;

public record PageResponse<T> (
        Collection<T> items,
        PaginationResponse pagination
) {

}
