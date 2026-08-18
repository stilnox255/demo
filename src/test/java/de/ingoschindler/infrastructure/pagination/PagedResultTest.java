package de.ingoschindler.infrastructure.pagination;

import de.ingoschindler.kernel.pagination.Page;
import de.ingoschindler.kernel.pagination.PageRequest;
import de.ingoschindler.kernel.pagination.PagedResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagedResultTest {

    @Test
    void metaIsDerivedFromThePageAndContentIsMapped() {
        var page = Page.of(List.of(1, 2, 3), new PageRequest(2, 10), 25L);

        var result = PagedResult.of(page, i -> "item-" + i);

        assertEquals(List.of("item-1", "item-2", "item-3"), result.items());
        assertEquals(2, result.meta().page());
        assertEquals(10, result.meta().pageSize());
        assertEquals(25L, result.meta().totalItems());
        assertEquals(3L, result.meta().totalPages());
    }

    @Test
    void emptyPageAndItsWireMetaBothReportZeroTotalPages() {
        var page = Page.<Integer>empty(new PageRequest(1, 25));

        var result = PagedResult.of(page, Object::toString);

        assertEquals(List.of(), result.items());
        assertEquals(0L, page.totalPages());
        assertEquals(0L, result.meta().totalPages());
    }
}
