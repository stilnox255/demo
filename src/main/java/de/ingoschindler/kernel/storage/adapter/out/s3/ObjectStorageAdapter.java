package de.ingoschindler.kernel.storage.adapter.out.s3;

import de.ingoschindler.kernel.storage.ObjectStoragePort;
import de.ingoschindler.kernel.storage.S3ObjectStore;
import de.ingoschindler.kernel.upload.UploadedFile;
import de.ingoschindler.kernel.storage.adapter.out.persistence.StorageRefJpaEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.io.InputStream;
import java.util.UUID;

/**
 * Default adapter for {@link ObjectStoragePort}: writes blobs to the injected
 * S3-compatible store and registers them in the {@code storage_ref} table.
 *
 * <p>Resolves the opaque {@code storage_ref} row id to the managed
 * {@link StorageRefJpaEntity} internally so the application layer never sees
 * JPA, nor the bucket/prefix/key detail behind an id.
 *
 * <p>The fault-tolerance annotations sit on this {@code @ApplicationScoped}
 * class and not on an S3 client interface (ADR-20). Interceptor state is kept
 * per intercepted bean instance, so annotations on a per-call proxy would give
 * every call its own fresh state and never accumulate anything. All values come
 * from configuration; see {@code application.properties}.
 */
@ApplicationScoped
public class ObjectStorageAdapter implements ObjectStoragePort {

    @Inject
    S3ObjectStore s3;

    static String extension(String fileName) {
        var dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    /**
     * {@code @Timeout} but deliberately no {@code @Retry}: {@code data} has
     * already been consumed by the time a failure surfaces, so a second attempt
     * would upload a truncated body and report success. Retrying a
     * non-replayable request is worse than failing it.
     */
    @Override
    @Timeout
    public String putBlob(String bucket, String prefix, String fileName, InputStream data, String contentType,
            long size) {
        var key = UUID.randomUUID() + extension(fileName);
        s3.put(bucket, prefix + key, data, contentType, size);
        return key;
    }

    @Override
    public UUID registerBlob(String bucket, String prefix, String key, String contentType, long size, String hash,
            String ownerId) {
        var jpa = new StorageRefJpaEntity();
        jpa.bucket = bucket;
        jpa.prefix = prefix;
        jpa.key = key;
        jpa.contentType = contentType;
        jpa.size = size;
        jpa.hash = hash;
        jpa.ownerId = ownerId;
        jpa.persist();
        return jpa.id;
    }

    @Override
    public UUID storeAndRegister(String bucket, String prefix, UploadedFile file, String hash, String ownerId) {
        try (InputStream stream = file.openStream()) {
            String key = putBlob(bucket, prefix, file.filename(), stream, file.contentType(), file.size());
            return registerBlob(bucket, prefix, key, file.contentType(), file.size(), hash, ownerId);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override
    @Timeout
    @Retry(retryOn = software.amazon.awssdk.core.exception.SdkException.class)
    public void delete(UUID storageRefId) {
        var jpa = resolve(storageRefId);
        s3.delete(jpa.bucket, jpa.fullKey());
        jpa.delete();
    }

    /**
     * Idempotent, so a bounded retry is safe here. Retries only on transport
     * failures — a retry on a "no such key" would just burn the budget to get
     * the same answer three times.
     */
    @Override
    @Timeout
    @Retry(retryOn = software.amazon.awssdk.core.exception.SdkException.class)
    public InputStream download(UUID storageRefId) {
        var jpa = resolve(storageRefId);
        return s3.get(jpa.bucket, jpa.fullKey());
    }

    static StorageRefJpaEntity resolve(UUID storageRefId) {
        var jpa = (StorageRefJpaEntity) StorageRefJpaEntity.findById(storageRefId);
        if (jpa == null) {
            throw new IllegalStateException("No storage_ref row for id " + storageRefId);
        }
        return jpa;
    }
}
