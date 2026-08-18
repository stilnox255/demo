package de.ingoschindler.demo.adapter.out.persistence;

import de.ingoschindler.demo.domain.DemoItem;

/**
 * Translates between the domain record and the JPA entity.
 *
 * <p>Hand-rolled, and worth the twenty lines: it is the seam that lets the
 * domain stay a plain record. A generator would need the entity's accessors to
 * match the record's, which they do not — {@code name()} is not
 * {@code getName()}.</p>
 */
final class DemoItemPersistenceMapper {

    private DemoItemPersistenceMapper() {
    }

    static DemoItem toDomain(DemoItemJpaEntity jpa) {
        DemoItem.Attachment attachment = jpa.attachmentStorageRefId == null
                ? null
                : new DemoItem.Attachment(jpa.attachmentStorageRefId, jpa.attachmentFileName, jpa.attachmentContentType,
                        jpa.attachmentSize == null ? 0L : jpa.attachmentSize);

        return new DemoItem(jpa.id, jpa.name, jpa.description, jpa.status, jpa.ownerId, attachment, jpa.createdAt,
                jpa.version);
    }

    /**
     * Copies the mutable state of {@code item} onto {@code jpa}. Does not touch
     * {@code id} or {@code version}: identity is fixed, and the version is owned
     * by Hibernate once the entity is managed.
     */
    static void copyInto(DemoItem item, DemoItemJpaEntity jpa) {
        jpa.name = item.name();
        jpa.description = item.description();
        jpa.status = item.status();
        jpa.ownerId = item.ownerId();
        jpa.createdAt = item.createdAt();
        item.findAttachment().ifPresentOrElse(attachment -> {
            jpa.attachmentStorageRefId = attachment.storageRefId();
            jpa.attachmentFileName = attachment.fileName();
            jpa.attachmentContentType = attachment.contentType();
            jpa.attachmentSize = attachment.size();
        }, () -> {
            jpa.attachmentStorageRefId = null;
            jpa.attachmentFileName = null;
            jpa.attachmentContentType = null;
            jpa.attachmentSize = null;
        });
    }
}
