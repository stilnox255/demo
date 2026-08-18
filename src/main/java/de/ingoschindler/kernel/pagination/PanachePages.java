package de.ingoschindler.kernel.pagination;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

import java.util.function.Function;

/**
 * Persistence-adapter helper: turn a {@link PanacheQuery} into a domain
 * {@link de.ingoschindler.kernel.pagination.Page Page&lt;T&gt;}.
 *
 * <p>This is the single sanctioned point of contact between Panache and the
 * domain pagination type. Persistence adapters (the only place {@link PanacheQuery}
 * is allowed) call {@link #from(PanacheQuery, PageRequest, Function)} and
 * return the resulting domain page. The Panache cursor never leaves the
 * adapter; the application layer only sees materialised domain types.
 */
public final class PanachePages {

    PanachePages() {
    }

    public static <E, T> de.ingoschindler.kernel.pagination.Page<T> from(PanacheQuery<E> query, PageRequest request,
            Function<E, T> mapper) {
        long totalItems = query.count();
        var pageSlice = query.page(Page.of(request.page() - 1, request.pageSize())).list();
        var content = pageSlice.stream().map(mapper).toList();
        return de.ingoschindler.kernel.pagination.Page.of(content, request, totalItems);
    }
}
