package de.ingoschindler.demo.adapter.out.persistence;

import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemStatus;
import de.ingoschindler.kernel.pagination.Page;
import de.ingoschindler.kernel.pagination.PageRequest;
import de.ingoschindler.kernel.pagination.PanachePages;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Panache implementation of {@link DemoItemRepository}.
 *
 * <p>Every filter is expressed in the query, never as a post-filter in Java: a
 * {@code list()} followed by a {@code .filter()} reads the whole table to throw
 * most of it away, and the wrongness only shows up once the table is big.</p>
 *
 * <p>{@link PanacheQuery} never leaves this class. Pagination is converted to the
 * kernel {@link Page} through {@link PanachePages}, which is the one sanctioned
 * point of contact between the two (ADR-02).</p>
 */
@ApplicationScoped
public class JpaDemoItemRepository implements DemoItemRepository {

    private static final Sort NEWEST_FIRST = Sort.by("createdAt", Sort.Direction.Descending).and("id");

    @Inject
    EntityManager entityManager;

    @Override
    public Optional<DemoItem> findByIdForOwner(UUID id, String ownerId) {
        return DemoItemJpaEntity
                .<DemoItemJpaEntity>find("id = :id and ownerId = :ownerId", Map.of("id", id, "ownerId", ownerId))
                .firstResultOptional().map(DemoItemPersistenceMapper::toDomain);
    }

    @Override
    public Optional<DemoItem> findByIdForSignedAccess(UUID id) {
        return DemoItemJpaEntity.<DemoItemJpaEntity>findByIdOptional(id).map(DemoItemPersistenceMapper::toDomain);
    }

    @Override
    public Page<DemoItem> findPageForOwner(PageRequest request, String ownerId) {
        PanacheQuery<DemoItemJpaEntity> query = DemoItemJpaEntity.find("ownerId", NEWEST_FIRST, ownerId);
        return PanachePages.from(query, request, DemoItemPersistenceMapper::toDomain);
    }

    @Override
    public List<DemoItem> findAllForOwner(String ownerId) {
        return DemoItemJpaEntity.<DemoItemJpaEntity>find("ownerId", NEWEST_FIRST, ownerId).list().stream()
                .map(DemoItemPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<DemoItem> findStaleDrafts(Instant threshold, int limit) {
        return DemoItemJpaEntity
                .<DemoItemJpaEntity>find("status = :status and createdAt < :threshold", NEWEST_FIRST,
                        Map.of("status", DemoItemStatus.DRAFT, "threshold", threshold))
                .page(0, Math.max(1, limit)).list().stream().map(DemoItemPersistenceMapper::toDomain).toList();
    }

    @Override
    public DemoItem save(DemoItem item) {
        DemoItemJpaEntity jpa = DemoItemJpaEntity.findById(item.id());
        if (jpa == null) {
            jpa = new DemoItemJpaEntity();
            jpa.id = item.id();
            DemoItemPersistenceMapper.copyInto(item, jpa);
            jpa.persist();
        } else {
            // The caller's version has to be checked explicitly. Hibernate only
            // compares the version it loaded itself, and that is the current row
            // — so an update built from a stale read would sail straight through.
            if (jpa.version != item.version()) {
                throw new OptimisticLockException(
                        "demo_item " + item.id() + " is at version " + jpa.version + ", caller sent " + item.version());
            }
            DemoItemPersistenceMapper.copyInto(item, jpa);
        }
        // Forces the INSERT/UPDATE (and any version conflict) to surface here,
        // inside the use case's transaction, instead of at commit time where no
        // mapper can turn it into a meaningful status code.
        entityManager.flush();
        return DemoItemPersistenceMapper.toDomain(jpa);
    }

    @Override
    public void delete(DemoItem item) {
        DemoItemJpaEntity.deleteById(item.id());
    }
}
