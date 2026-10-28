package by.piskunou.teletronics.api.to.response.collection;

import java.util.Collection;

public record CollectionResponse<T>(
        Collection<T> items
) {
}
