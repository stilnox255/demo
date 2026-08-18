package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.AttachFileToDemoItemCommand;
import de.ingoschindler.demo.application.port.in.AttachFileToDemoItemUseCase;
import de.ingoschindler.demo.application.port.in.DemoItemResult;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.kernel.storage.ObjectStoragePort;
import de.ingoschindler.kernel.storage.S3Prefixes;
import de.ingoschindler.kernel.upload.UploadedFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Stores an uploaded file and records it on the item.
 *
 * <p>Deliberately <b>not</b> {@code @Transactional} as a whole. The upload is a
 * network transfer of up to the configured body limit; holding a JDBC connection
 * open for its duration is how a pool of 20 connections is exhausted by 20 slow
 * clients. So the bytes go out first, uncommitted, and only the bookkeeping runs
 * in a transaction — see {@link DemoItemAttachmentRecorder}.</p>
 *
 * <p>The recorder is a separate bean and not a private method here, because
 * {@code @Transactional} is a CDI interceptor: a self-call would not go through
 * the proxy and the annotation would silently do nothing. A safeguard that
 * cannot fire is worse than none, because it also stops anyone from looking.</p>
 *
 * <p>Failure order is chosen: a blob written but never recorded is an orphan
 * costing disk. A row recorded before the blob lands is a broken download.</p>
 */
@ApplicationScoped
public class AttachFileToDemoItemService implements AttachFileToDemoItemUseCase {

    @Inject
    DemoItemRepository repository;

    @Inject
    ObjectStoragePort objectStorage;

    @Inject
    DemoItemAttachmentRecorder recorder;

    @ConfigProperty(name = "s3.bucket.files")
    String bucket;

    @Override
    public DemoItemResult attach(AttachFileToDemoItemCommand command) {
        // Read once, before the upload: proves the caller owns the item and gives
        // a 404 without having paid for a transfer first.
        DemoItem item = repository.findByIdForOwner(command.id(), command.ownerId())
                .orElseThrow(() -> new DemoItemNotFoundException(command.id()));

        UploadedFile file = command.file();
        String hash = file.contentHash();
        String key;
        try (var stream = file.openStream()) {
            key = objectStorage.putBlob(bucket, S3Prefixes.ATTACHMENTS, file.filename(), stream, file.contentType(),
                    file.size());
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }

        return new DemoItemResult(recorder.record(item.id(), command.ownerId(), bucket, key, file, hash));
    }
}
