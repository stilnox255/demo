package de.ingoschindler.kernel.pagination;

/**
 * Inbound pagination parameters for use-case calls.
 *
 * <p>One-based {@code page} index ({@code 1} is the first page) and a hard
 * {@code maxPageSize} cap to protect repositories from caller-supplied huge
 * pages. Defaults are deliberately conservative ({@link #DEFAULT_PAGE_SIZE} =
 * 25, {@link #MAX_PAGE_SIZE} = 100): an unbounded page size is a caller-supplied
 * denial of service, so the cap is applied here rather than trusted per query.
 */
public record PageRequest(int page, int pageSize) {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 100;

    public PageRequest {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
    }

    public static PageRequest of(Integer page, Integer pageSize) {
        return new PageRequest(page == null ? 1 : page, pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
    }

    public static PageRequest first() {
        return new PageRequest(1, DEFAULT_PAGE_SIZE);
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
