package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.DownloadDemoItemAttachmentResult;
import de.ingoschindler.demo.application.port.in.DownloadDemoItemAttachmentUseCase;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.kernel.storage.ObjectStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Streams the attachment of one item.
 *
 * <p>No transaction: one read and a blob fetch. Holding a database connection
 * open while the response body streams to a slow client would be the mistake
 * here, and it is the most common way a "read-only" endpoint takes out a pool.</p>
 */
@ApplicationScoped
public class DownloadDemoItemAttachmentService implements DownloadDemoItemAttachmentUseCase {

    @Inject
    DemoItemRepository repository;

    @Inject
    ObjectStoragePort objectStorage;

    @Override
    public DownloadDemoItemAttachmentResult download(UUID id) {
        var attachment = repository.findByIdForSignedAccess(id).flatMap(DemoItem::findAttachment)
                .orElseThrow(() -> new DemoItemNotFoundException(id));

        return new DownloadDemoItemAttachmentResult(objectStorage.download(attachment.storageRefId()),
                attachment.fileName(), attachment.contentType(), attachment.size());
    }
}
