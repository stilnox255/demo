package de.ingoschindler.kernel.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * REST-layer pagination envelope. Wraps a slice of items together with the
 * pagination metadata clients need to render pagers.
 */
public record PagedResult<T>(PageMeta meta, List<T> items) {

    /**
     * Builds the REST envelope from an application-layer {@link Page}, mapping its content to DTOs.
     *
     * <p>The one sanctioned conversion between the two pagination types. Every inbound REST adapter
     * returning a paginated list goes through here rather than assembling {@code PageMeta} itself —
     * hand-assembling {@code PageMeta} per resource is one chance per resource for the meta to
     * disagree with the slice it describes.
     */
    public static <T, R> PagedResult<R> of(Page<T> page, Function<T, R> mapper) {
        return new PagedResult<>(PageMeta.of(page.page(), page.pageSize(), page.totalItems()),
                page.content().stream().map(mapper).toList());
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        List<R> mappedItems = this.items.stream().map(mapper).toList();
        return new PagedResult<>(this.meta, mappedItems);
    }
}
