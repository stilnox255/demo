package de.ingoschindler.kernel.storage.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * JPA mapping for the {@code storage_ref} table — the canonical persistence
 * surface of the {@code kernel.storage} capability (ADR-17).
 *
 * <p>A normalized table rather than an {@code @Embeddable} on every owner:
 * embedding would copy five columns into each aggregate that holds a file and
 * leave no place to catalogue a blob that outlives its owner.</p>
 *
 * <p>No BC's JPA entity holds a {@code @ManyToOne} onto this type. Callers
 * store only the opaque row id ({@link UUID}) and reach the capability through
 * {@code ObjectStoragePort}, which accepts and returns that id at the port
 * boundary (ADR-03). That is what keeps a schema change here from rippling into
 * every owning aggregate.</p>
 *
 * <p>{@code @Entity(name = "StorageRef")} is intentionally absent: there are no
 * JPQL queries against this entity, all access goes through Panache's
 * {@code findById} / {@code persist}.</p>
 */
@Entity
@Table(name = "storage_ref")
public class StorageRefJpaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(nullable = false)
    public String bucket;

    @Column(nullable = false)
    public String prefix;

    /**
     * The filename portion of the S3 key, without the prefix.
     */
    @Column(name = "key_name", nullable = false)
    public String key;

    @Column(name = "content_type")
    public String contentType;

    @Column
    public Long size;

    @Column(nullable = false)
    public String hash;

    @Column(name = "owner_id", nullable = false)
    public String ownerId;

    /**
     * Returns the full S3 object key: prefix + key.
     */
    public String fullKey() {
        return prefix + key;
    }
}
