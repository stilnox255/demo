package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemStatus;
import de.ingoschindler.kernel.pagination.Page;
import de.ingoschindler.kernel.pagination.PageRequest;
import jakarta.persistence.OptimisticLockException;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Hand-written fake for {@link DemoItemRepository}.
 *
 * <p>A fake rather than a mock: the use-case tests below assert on behaviour
 * across several calls (save, then read back, then save again with a stale
 * version), and expressing that with stubs would encode the implementation's call
 * sequence into the test. This class also models the one behaviour that matters
 * for correctness — the version check — so a test can cover the 409 path without
 * a database.</p>
 *
 * <p>It is a fake and not a second implementation: no CDI annotation, test scope
 * only.</p>
 */
public class InMemoryDemoItemRepository implements DemoItemRepository {

    private final Map<UUID, DemoItem> items = new LinkedHashMap<>();

    @Override
    public Optional<DemoItem> findByIdForOwner(UUID id, String ownerId) {
        return Optional.ofNullable(items.get(id)).filter(item -> item.isOwnedBy(ownerId));
    }

    @Override
    public Optional<DemoItem> findByIdForSignedAccess(UUID id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public Page<DemoItem> findPageForOwner(PageRequest request, String ownerId) {
        List<DemoItem> all = findAllForOwner(ownerId);
        List<DemoItem> slice = all.stream().skip(request.offset()).limit(request.pageSize()).toList();
        return Page.of(slice, request, all.size());
    }

    @Override
    public List<DemoItem> findAllForOwner(String ownerId) {
        return items.values().stream().filter(item -> item.isOwnedBy(ownerId))
                .sorted(Comparator.comparing(DemoItem::createdAt).reversed()).toList();
    }

    @Override
    public List<DemoItem> findStaleDrafts(Instant threshold, int limit) {
        return items.values().stream().filter(item -> item.status() == DemoItemStatus.DRAFT)
                .filter(item -> item.createdAt().isBefore(threshold)).limit(limit).toList();
    }

    @Override
    public DemoItem save(DemoItem item) {
        DemoItem existing = items.get(item.id());
        if (existing != null && existing.version() != item.version()) {
            throw new OptimisticLockException(
                    item.id() + " is at version " + existing.version() + ", caller sent " + item.version());
        }
        DemoItem stored = item.atVersion(existing == null ? 0L : existing.version() + 1);
        items.put(stored.id(), stored);
        return stored;
    }

    @Override
    public void delete(DemoItem item) {
        items.remove(item.id());
    }
}
