package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.DeleteDemoItemUseCase;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.kernel.storage.ObjectStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

/**
 * Deletes an item and the blob behind its attachment.
 *
 * <p>Row first, blob second, both inside the transaction. The order matters and
 * neither order is free of failure modes: deleting the blob first and then
 * failing the commit would leave a row pointing at nothing, which breaks reads.
 * This way a failure after the commit leaves an orphaned blob — invisible to
 * callers, reclaimable by a sweep, and cheap. Prefer the failure that costs disk
 * over the one that costs correctness.</p>
 */
@ApplicationScoped
public class DeleteDemoItemService implements DeleteDemoItemUseCase {

    @Inject
    DemoItemRepository repository;

    @Inject
    ObjectStoragePort objectStorage;

    @Override
    @Transactional
    public void delete(UUID id, String ownerId) {
        DemoItem item = repository.findByIdForOwner(id, ownerId).orElseThrow(() -> new DemoItemNotFoundException(id));

        repository.delete(item);
        item.findAttachment().ifPresent(attachment -> objectStorage.delete(attachment.storageRefId()));
    }
}
