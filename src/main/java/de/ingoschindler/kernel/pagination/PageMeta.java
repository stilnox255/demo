package de.ingoschindler.kernel.pagination;

public record PageMeta(int page, int pageSize, long totalItems, long totalPages) {
    public static PageMeta of(int page, int pageSize, long totalItems) {
        int safePageSize = Math.max(1, pageSize);
        return new PageMeta(Math.max(1, page), safePageSize, totalItems, Page.totalPages(totalItems, safePageSize));
    }
}
