package de.ingoschindler.demo.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The aggregate this starter is built around: a small owned entity with a
 * lifecycle, an optional file attachment and a version for optimistic locking.
 *
 * <p>A pure record — no JPA, no JAX-RS, no Quarkus. That is the whole point of
 * the layering (ADR-01): every rule below is testable with plain values and no
 * test doubles, and the persistence mapping in
 * {@code adapter.out.persistence} can change without touching this file.</p>
 *
 * <p>Immutable, so a transition returns a new instance instead of mutating in
 * place. {@code version} rides along because the caller has to send back the
 * value it read for optimistic locking to mean anything.</p>
 *
 * @param version {@code 0} for an instance that has never been persisted
 */
public record DemoItem(UUID id, String name, String description, DemoItemStatus status, String ownerId,
        Attachment attachment, Instant createdAt, long version) {

    public static final int NAME_MAX_LENGTH = 120;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    /**
     * Metadata of the stored file, if any. Holds the {@code storage_ref} row id
     * plus the two fields needed to serve a download without a second lookup
     * against the storage catalogue.
     */
    public record Attachment(UUID storageRefId, String fileName, String contentType, long size) {

        public Attachment {
            Objects.requireNonNull(storageRefId, "storageRefId");
            Objects.requireNonNull(fileName, "fileName");
        }
    }

    public DemoItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        name = requireName(name);
        description = requireDescription(description);
        ownerId = requireOwner(ownerId);
    }

    /**
     * Factory for a brand-new item. Identity is assigned here rather than by the
     * database so the use case can publish an event carrying the id before the
     * transaction commits, and so a test never needs a database to build one.
     */
    public static DemoItem create(String name, String description, String ownerId, Instant now) {
        return new DemoItem(UUID.randomUUID(), name, description, DemoItemStatus.DRAFT, ownerId, null, now, 0L);
    }

    public DemoItem withDetails(String newName, String newDescription) {
        return new DemoItem(id, newName, newDescription, status, ownerId, attachment, createdAt, version);
    }

    /**
     * Moves to {@code target}. {@link DemoItemStatus#ARCHIVED} is terminal, so
     * leaving it is rejected here rather than in the use case — an invariant
     * enforced in one place cannot be forgotten by the next caller.
     */
    public DemoItem withStatus(DemoItemStatus target) {
        Objects.requireNonNull(target, "target");
        if (status == DemoItemStatus.ARCHIVED && target != DemoItemStatus.ARCHIVED) {
            throw new IllegalArgumentException("An archived item cannot return to " + target);
        }
        return new DemoItem(id, name, description, target, ownerId, attachment, createdAt, version);
    }

    public DemoItem withAttachment(Attachment newAttachment) {
        Objects.requireNonNull(newAttachment, "newAttachment");
        return new DemoItem(id, name, description, status, ownerId, newAttachment, createdAt, version);
    }

    /** The version the caller claims to have read, for an optimistic update. */
    public DemoItem atVersion(long expectedVersion) {
        return new DemoItem(id, name, description, status, ownerId, attachment, createdAt, expectedVersion);
    }

    public Optional<Attachment> findAttachment() {
        return Optional.ofNullable(attachment);
    }

    public boolean isOwnedBy(String candidate) {
        return ownerId.equals(candidate);
    }

    /** True if this item is still a draft and older than {@code threshold}. */
    public boolean isStaleDraft(Instant threshold) {
        return status == DemoItemStatus.DRAFT && createdAt.isBefore(threshold);
    }

    private static String requireName(String value) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("name must be at most " + NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String requireDescription(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        return value;
    }

    private static String requireOwner(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        return value;
    }
}
