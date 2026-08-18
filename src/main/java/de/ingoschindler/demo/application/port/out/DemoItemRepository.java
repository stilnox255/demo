package de.ingoschindler.demo.application.port.out;

import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.kernel.pagination.Page;
import de.ingoschindler.kernel.pagination.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for {@link DemoItem} persistence.
 *
 * <p>Every read is owner-scoped at the port level. There is deliberately no
 * {@code findById(UUID)} to reach for by accident: an ownership check that lives
 * in the caller is an ownership check somebody forgets, which is how an IDOR
 * ships. The filter belongs in the query, not in an {@code if} after it.</p>
 */
public interface DemoItemRepository {

    Optional<DemoItem> findByIdForOwner(UUID id, String ownerId);

    /**
     * Lookup <em>without</em> an owner filter, for the one path whose
     * authorization is a signed token rather than the OIDC principal: the
     * attachment download (ADR-19). Named so that using it looks like the
     * exception it is — the caller must have validated the token for exactly
     * this id first.
     */
    Optional<DemoItem> findByIdForSignedAccess(UUID id);

    Page<DemoItem> findPageForOwner(PageRequest request, String ownerId);

    /**
     * All items of one owner, newest first. Backs the cached summary snapshot,
     * which is why it is unpaginated — bounded by the fact that one owner's
     * items are a small set.
     */
    List<DemoItem> findAllForOwner(String ownerId);

    /**
     * Draft items created before {@code threshold}, newest first, at most
     * {@code limit} rows. Bounded on purpose: the archive job must do a
     * predictable amount of work per run, however long it has been since the
     * last one succeeded.
     */
    List<DemoItem> findStaleDrafts(Instant threshold, int limit);

    /**
     * Insert or update. The {@code version} carried by {@code item} is the
     * optimistic-lock token; a stale value makes this throw and the request
     * surface as 409.
     */
    DemoItem save(DemoItem item);

    void delete(DemoItem item);
}
