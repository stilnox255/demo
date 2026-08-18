package de.ingoschindler.infrastructure.pagination;

import de.ingoschindler.kernel.pagination.PageMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageMetaTest {

    @Test
    void totalPagesCalculated() {
        var meta = PageMeta.of(1, 10, 25);
        assertEquals(3, meta.totalPages());
    }

    @Test
    void exactDivisionNoExtraPage() {
        var meta = PageMeta.of(1, 10, 20);
        assertEquals(2, meta.totalPages());
    }

    @Test
    void zeroItemsIsZeroPages() {
        var meta = PageMeta.of(1, 10, 0);
        assertEquals(0, meta.totalPages());
    }

    @Test
    void pageSizeZeroDefaultsToOne() {
        var meta = PageMeta.of(1, 0, 5);
        assertEquals(5, meta.totalPages());
    }
}
