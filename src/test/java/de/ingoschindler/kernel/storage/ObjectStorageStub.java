package de.ingoschindler.kernel.storage;

import de.ingoschindler.kernel.storage.adapter.out.persistence.StorageRefJpaEntity;
import de.ingoschindler.kernel.upload.UploadedFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

/**
 * Stubs an {@link ObjectStoragePort} mock so it registers a real
 * {@code storage_ref} row instead of talking to S3, across every write shape
 * the port offers: the split {@link ObjectStoragePort#putBlob} /
 * {@link ObjectStoragePort#registerBlob} pair and the one-shot
 * {@link ObjectStoragePort#storeAndRegister}.
 *
 * <p>Stubs <em>every</em> write shape, not only the one the test at hand needs.
 * A mock that covers just the current call site stops matching the day a use case
 * switches from {@code storeAndRegister} to the split pair — and an unmatched
 * Mockito stub returns null rather than failing, so the test breaks somewhere
 * else entirely.
 */
public final class ObjectStorageStub {

    private ObjectStorageStub() {
    }

    /** Stubs every write method on {@code mock}. Safe to call once per test setup. */
    public static void stubWrites(ObjectStoragePort mock) {
        doAnswer(inv -> UUID.randomUUID() + ".bin").when(mock).putBlob(any(), any(), any(), any(), any(), anyLong());

        doAnswer(inv -> persistRef(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3),
                inv.getArgument(4), inv.getArgument(5), inv.getArgument(6))).when(mock)
                .registerBlob(any(), any(), any(), any(), anyLong(), any(), any());

        doAnswer(inv -> {
            UploadedFile file = inv.getArgument(2);
            return persistRef(inv.getArgument(0), inv.getArgument(1), UUID.randomUUID() + ".bin", file.contentType(),
                    file.size(), inv.getArgument(3), inv.getArgument(4));
        }).when(mock).storeAndRegister(any(), any(), any(), any(), any());
    }

    /** Persists a {@code storage_ref} row and returns its id. */
    public static UUID persistRef(String bucket, String prefix, String key, String contentType, long size, String hash,
            String ownerId) {
        var ref = new StorageRefJpaEntity();
        ref.bucket = bucket;
        ref.prefix = prefix;
        ref.key = key;
        ref.contentType = contentType;
        ref.size = size;
        ref.hash = hash;
        ref.ownerId = ownerId;
        ref.persist();
        return ref.id;
    }
}
