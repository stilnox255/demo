package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.kernel.storage.ObjectStoragePort;
import de.ingoschindler.kernel.storage.S3Prefixes;
import de.ingoschindler.kernel.upload.UploadedFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

/**
 * The transactional half of attaching a file: catalogue the blob in
 * {@code storage_ref} and point the item at it.
 *
 * <p>Exists as its own bean purely so {@code @Transactional} is reached through
 * the CDI proxy — see the note in {@link AttachFileToDemoItemService}. Not a
 * general-purpose service; it has exactly one caller on purpose.</p>
 */
@ApplicationScoped
public class DemoItemAttachmentRecorder {

    @Inject
    DemoItemRepository repository;

    @Inject
    ObjectStoragePort objectStorage;

    @Transactional
    public DemoItem record(UUID itemId, String ownerId, String bucket, String key, UploadedFile file, String hash) {
        // Re-read inside the transaction: the item may have been deleted while the
        // upload was in flight, and writing the reference then would resurrect it.
        DemoItem item = repository.findByIdForOwner(itemId, ownerId)
                .orElseThrow(() -> new DemoItemNotFoundException(itemId));

        UUID storageRefId = objectStorage.registerBlob(bucket, S3Prefixes.ATTACHMENTS, key, file.contentType(),
                file.size(), hash, ownerId);

        return repository.save(item.withAttachment(
                new DemoItem.Attachment(storageRefId, file.filename(), file.contentType(), file.size())));
    }
}
