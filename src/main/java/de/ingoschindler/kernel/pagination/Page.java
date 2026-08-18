package de.ingoschindler.kernel.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Outbound page slice returned by repositories and use cases.
 *
 * <p>Generic over the domain type the caller asked for; never over a JPA
 * entity. Adapters (Panache, JDBC, …) build a {@code Page} from their internal
 * cursor representation and hand it to the application layer through an
 * out-port. Use cases pass it through to inbound adapters, optionally mapping
 * the contents via {@link #map(Function)}.
 *
 * <p>Replaces {@code io.quarkus.hibernate.orm.panache.PanacheQuery} as the
 * application-layer pagination type. {@code PanacheQuery} stays an internal
 * detail of the persistence adapter.
 *
 * @param content    the items on this page (already materialised)
 * @param page       one-based page index that produced this slice
 * @param pageSize   maximum number of items per page
 * @param totalItems total items across all pages
 */
public record Page<T>(List<T> content, int page, int pageSize, long totalItems) {

    public static <T> Page<T> of(List<T> content, PageRequest request, long totalItems) {
        return new Page<>(content, request.page(), request.pageSize(), totalItems);
    }

    public static <T> Page<T> empty(PageRequest request) {
        return of(List.of(), request, 0L);
    }

    /**
     * The one page-count formula in the codebase; {@link PageMeta#of} calls it for the wire value.
     * Zero items means zero pages, matching Spring Data and leaving no special case to disagree about.
     */
    public static long totalPages(long totalItems, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        return (totalItems + safePageSize - 1) / safePageSize;
    }

    public long totalPages() {
        return totalPages(this.totalItems, this.pageSize);
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(this.content.stream().map(mapper).toList(), this.page, this.pageSize, this.totalItems);
    }

    public boolean isEmpty() {
        return this.content.isEmpty();
    }

    public int size() {
        return this.content.size();
    }
}
