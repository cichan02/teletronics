package by.piskunou.teletronics.api.to.response;

public record PaginationResponse(
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
}
