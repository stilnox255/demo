package de.ingoschindler.demo.adapter.in.rest;

import de.ingoschindler.demo.adapter.in.rest.dto.DemoItemMapper;
import de.ingoschindler.demo.adapter.in.rest.dto.DemoItemSummary;
import de.ingoschindler.demo.application.port.in.DemoItemQueryPort;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Cached summary snapshot per owner, with the HTTP validator in the same entry
 * (ADR-41).
 *
 * <p>It sits in the inbound REST adapter and not under {@code adapter.out}: what
 * is cached is a wire representation together with its {@code ETag}, so this is a
 * response cache belonging to the endpoint, not a gateway to an external system.
 * It depends on the query in-port only, like any other inbound adapter.</p>
 *
 * <p>Backed by Redis in deployed environments and by the in-process cache in
 * tests — one annotation, switched by {@code quarkus.cache.type}, no second code
 * path. The endpoint behind it is the one worth caching: small, unpaginated,
 * polled by every client. Paginated queries deliberately do not come through
 * here; caching page 7 of a filter nobody repeats fills the cache with entries
 * that are never read again.</p>
 *
 * <p><b>Exactly one cached method, and no callers inside this class.</b>
 * {@code @CacheResult} is a CDI interceptor, so a self-call does not pass through
 * the proxy: a convenience wrapper here would silently bypass the cache while
 * still returning correct answers, which is a performance bug with no symptom.
 * Everything that needs a fallback or a filter lives in
 * {@link DemoItemSummaries}, which reaches this bean by injection.</p>
 *
 * <p><b>The {@code -v1} suffix in the cache name is load-bearing.</b> Redis stores
 * the value as JSON. Change the shape of {@link Snapshot} or of
 * {@link DemoItemSummary} and a rolling deploy will read entries written by the
 * previous version, fail to decode them, and answer 500 until the keys expire or
 * are flushed by hand. Bump the suffix in the same commit as the shape change: a
 * new key is a guaranteed miss, and the old entries rot away harmlessly.</p>
 *
 * <p><b>No {@code expire-after-write}.</b> A TTL as a safety net only means a
 * missing invalidation shows up later and looks like something else. The write
 * paths invalidate explicitly; a missing one is a bug to fix, not to time out.</p>
 */
@ApplicationScoped
public class DemoItemSummariesCache {

    /**
     * Referenced by {@link DemoItemSummaries} rather than repeated as a literal, so
     * bumping the version suffix cannot miss a usage.
     */
    public static final String CACHE_NAME = "demo-item-summaries-v1";

    @Inject
    DemoItemQueryPort query;

    /**
     * The cached snapshot for one owner. Keyed by owner because the payload is
     * owner-scoped — a shared entry would hand one caller another caller's rows.
     */
    @CacheResult(cacheName = CACHE_NAME)
    public Snapshot snapshot(@CacheKey String ownerId) {
        return build(ownerId);
    }

    /**
     * The same snapshot without touching the cache. Not intercepted, so calling it
     * from anywhere is safe; it exists as the fallback for a cache outage.
     */
    public Snapshot uncachedSnapshot(String ownerId) {
        return build(ownerId);
    }

    private Snapshot build(String ownerId) {
        List<DemoItemSummary> items = query.allForOwner(ownerId).stream().map(DemoItemMapper::toSummary).toList();
        return new Snapshot(etagOf(items), items);
    }

    /**
     * Content hash plus item count. Moves exactly when the payload moves, which is
     * what makes it usable as an {@code ETag}: any field edit changes the hash, any
     * add or remove changes the count.
     */
    private static String etagOf(List<DemoItemSummary> items) {
        return Integer.toHexString(items.hashCode()) + "-" + items.size();
    }

    /**
     * Cache value: the HTTP validator and the payload it describes, together.
     *
     * <p>Together on purpose — one entry means one invalidation. An {@code ETag}
     * cached separately from its body is a race with a wrong answer at the end of
     * it.</p>
     */
    public record Snapshot(String etag, List<DemoItemSummary> items) {
    }
}
