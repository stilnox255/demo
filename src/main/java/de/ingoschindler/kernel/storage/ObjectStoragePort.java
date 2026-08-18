package de.ingoschindler.kernel.storage;

import de.ingoschindler.kernel.upload.UploadedFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Outbound port for blob storage and its registry of references (ADR-15).
 *
 * <p>One contract for both halves of storing a file: the bytes go to an
 * S3-compatible store, the metadata to the {@code storage_ref} table. Two
 * parallel facades for the same job is the failure mode this port exists to
 * prevent — every caller that needs a blob goes through here.</p>
 *
 * <p>Blobs are identified by the opaque {@code storage_ref} row id
 * ({@link UUID}), so the application layer never sees the
 * {@code StorageRefJpaEntity} mapping, nor the bucket/prefix/key/hash detail
 * behind it. A consuming BC wraps that id in its own domain-local reference
 * type so the raw id never crosses a boundary untyped.</p>
 *
 * <p>Writing is split into {@link #putBlob} (bytes only) and
 * {@link #registerBlob} (row only) so a caller can push the bytes
 * <em>outside</em> a database transaction and record the reference inside one —
 * an upload must never hold a JDBC connection open for the duration of a
 * network transfer. {@link #storeAndRegister} keeps the one-shot path for
 * callers that genuinely want both side effects at once.</p>
 */
public interface ObjectStoragePort {

    /**
     * Upload bytes to the underlying blob store. Returns the generated S3 key
     * (without the prefix) the bytes were written to.
     */
    String putBlob(String bucket, String prefix, String fileName, InputStream data, String contentType, long size);

    /**
     * Persist a {@code storage_ref} row cataloguing an already-uploaded blob.
     * Caller passes the key returned by {@link #putBlob}. Returns the row id.
     */
    UUID registerBlob(String bucket, String prefix, String key, String contentType, long size, String hash,
            String ownerId);

    /**
     * Convenience: upload {@code file} and register a {@code storage_ref} row
     * for it in a single call. Equivalent to {@link #putBlob} followed by
     * {@link #registerBlob}.
     */
    UUID storeAndRegister(String bucket, String prefix, UploadedFile file, String hash, String ownerId);

    InputStream download(UUID storageRefId);

    /**
     * Delete the blob and its {@code storage_ref} catalogue row. There is no
     * separate "delete the blob but keep the row" operation — a row without a
     * backing blob is never useful, so the two always go together.
     */
    void delete(UUID storageRefId);
}
