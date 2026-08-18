package de.ingoschindler.demo.adapter.out.persistence;

import de.ingoschindler.demo.domain.DemoItemStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code demo_item}. Lives in {@code adapter.out.persistence}
 * and nowhere else: {@code extends PanacheEntityBase} is an implementation
 * detail of this package, not a type the application layer may see (ADR-01,
 * enforced by ArchUnit).
 *
 * <p>No {@code equals}/{@code hashCode}: these instances are only ever handled
 * inside one persistence context, and a generated-id entity with the default
 * identity semantics behaves correctly there. Adding a business-key pair here
 * would be ceremony with a real trap attached.</p>
 *
 * <p>The attachment fields are denormalized copies of what the
 * {@code storage_ref} row already holds. That is a deliberate trade (ADR-03):
 * serving a download would otherwise need a second lookup into a table this BC
 * does not own, on the hottest read path it has.</p>
 */
@Entity
@Table(name = "demo_item")
public class DemoItemJpaEntity extends PanacheEntityBase {

    /**
     * Assigned by the domain, not generated here. {@code DemoItem.create} mints the
     * UUID so the use case can publish an event carrying the id before the
     * transaction commits. Leaving {@code @GeneratedValue} on top of that would
     * make Hibernate treat a pre-populated id as a detached instance and reject
     * the insert.
     */
    @Id
    public UUID id;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(nullable = false, length = 2000)
    public String description;

    /**
     * {@code EnumType.STRING}, never {@code ORDINAL}: an ordinal column silently
     * remaps every existing row the day someone reorders the enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public DemoItemStatus status;

    @Column(name = "owner_id", nullable = false)
    public String ownerId;

    @Column(name = "attachment_storage_ref_id")
    public UUID attachmentStorageRefId;

    @Column(name = "attachment_file_name")
    public String attachmentFileName;

    @Column(name = "attachment_content_type")
    public String attachmentContentType;

    @Column(name = "attachment_size")
    public Long attachmentSize;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /**
     * Optimistic-lock token. Hibernate compares and bumps it on every flush; a
     * stale value fails the UPDATE and surfaces as 409 rather than as a lost
     * write nobody sees.
     */
    @Version
    @Column(nullable = false)
    public long version;
}
