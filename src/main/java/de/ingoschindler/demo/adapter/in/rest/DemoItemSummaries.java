package de.ingoschindler.demo.adapter.in.rest;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reads and invalidates the summary cache, and survives it being down.
 *
 * <p>A separate bean from {@link DemoItemSummariesCache} for one specific reason:
 * the cached method is intercepted, and an interceptor only fires when the call
 * arrives through the CDI proxy. Wrapping it inside its own class would be a
 * self-call — no interception, no caching, no symptom. So the wrapper lives one
 * bean over and injects the other.</p>
 *
 * <p><b>Why invalidation is programmatic rather than {@code @CacheInvalidateAll}
 * on the write endpoints.</b> The annotation is the idiomatic choice and was the
 * first implementation. It also makes every write depend on the cache being
 * reachable: {@code RedisCacheImpl.invalidate} has no failure recovery at all, so
 * with Redis down the item is created and the response is still a 500 — the
 * interceptor fails after the use case succeeded. Verified by stopping the Redis
 * container and watching a create turn into a 500. A cache outage must not become a
 * write outage, so invalidation goes through {@link CacheManager} where the failure
 * can be caught and logged.</p>
 *
 * <p><b>What the read fallback does and does not add.</b> Less than it looks:
 * {@code RedisCacheImpl.get} already recovers by recomputing, but only for the two
 * failures its own {@code isRecomputableError} lists — {@link java.net.ConnectException}
 * and a busy connection pool. A plain outage is therefore handled by the platform,
 * and this wrapper never fires for it.
 *
 * <p>What is left uncovered is the failure that actually caused an outage in the
 * project this pattern came from: a <em>decode</em> error, when a rolling deploy
 * reads entries written by the previous version of the value type. That is not in
 * {@code isRecomputableError}, so it propagates and the endpoint answers 500 until
 * the keys are flushed by hand. Versioning the cache name (see
 * {@link DemoItemSummariesCache}) is the fix; this fallback is the seatbelt for the
 * deploy where someone forgot to bump it.</p>
 *
 * <p>Both methods catch {@link RuntimeException} broadly, which is otherwise a smell
 * and here is the point: the remaining failure modes are many — a decode error, an
 * auth failure after a password rotation, a command timeout — and the right answer
 * to every one of them is the same. Degrade, and say so in the log.</p>
 */
@ApplicationScoped
public class DemoItemSummaries {

    private static final Logger LOGGER = Logger.getLogger(DemoItemSummaries.class);

    @Inject
    DemoItemSummariesCache cache;

    @Inject
    CacheManager cacheManager;

    /**
     * The owner's snapshot, from the cache when it answers and from the database
     * when it does not. See the class comment for which failures reach this
     * {@code catch} and which the cache extension already handles itself.
     */
    public DemoItemSummariesCache.Snapshot snapshot(String ownerId) {
        try {
            return cache.snapshot(ownerId);
        } catch (RuntimeException e) {
            LOGGER.warnf(e, "cache_degraded cache=%s operation=read outcome=served_from_database",
                    DemoItemSummariesCache.CACHE_NAME);
            return cache.uncachedSnapshot(ownerId);
        }
    }

    /**
     * Drops this owner's entry after a write.
     *
     * <p>Per owner and not the whole cache: the key is the {@code ownerId} that
     * {@code @CacheKey} derived, so one caller's write leaves every other caller's
     * entry warm. If that key derivation is ever wrong the invalidation silently
     * does nothing and the endpoint serves stale data, which is why there is an
     * integration case asserting that a write moves the {@code ETag}.</p>
     */
    public void invalidate(String ownerId) {
        withCache(handle -> handle.invalidate(ownerId).await().indefinitely(), "owner_scoped=true");
    }

    /**
     * Drops every entry. For a write that crosses owners — the archive job — where
     * there is no single key to target.
     */
    public void invalidateAll() {
        withCache(handle -> handle.invalidateAll().await().indefinitely(), "owner_scoped=false");
    }

    private void withCache(Consumer<Cache> action, String context) {
        try {
            Optional<Cache> handle = cacheManager.getCache(DemoItemSummariesCache.CACHE_NAME);
            if (handle.isEmpty()) {
                // Only reachable with caching switched off entirely, where there is
                // nothing to invalidate and nothing to warn about.
                return;
            }
            action.accept(handle.get());
        } catch (RuntimeException e) {
            LOGGER.warnf(e, "cache_invalidation_failed cache=%s %s outcome=write_still_committed",
                    DemoItemSummariesCache.CACHE_NAME, context);
        }
    }
}
