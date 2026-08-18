package de.ingoschindler.kernel.upload;

import de.ingoschindler.kernel.hash.Sha256;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

/**
 * Transport-agnostic descriptor for an inbound file payload.
 *
 * <p>Replaces {@code org.jboss.resteasy.reactive.multipart.FileUpload} in
 * application-layer and out-port signatures so the use cases and ports stay
 * decoupled from JAX-RS. Inbound adapters (REST resources, scheduled jobs,
 * worker callbacks) construct an {@code UploadedFile} from their transport
 * representation; the application layer consumes it through this record.
 *
 * <p>{@code content} is a supplier so the underlying stream can be opened
 * lazily (and re-opened if the use case needs to read the bytes twice, e.g.
 * once for hashing and once for upload).
 */
public record UploadedFile(String filename, String contentType, long size, Supplier<InputStream> content) {

    public InputStream openStream() {
        return content.get();
    }

    /**
     * Lowercase-hex SHA-256 of the content, the value {@code storage_ref} rows
     * are keyed by. Opens and closes its own stream, so callers can still read
     * the bytes again for the upload itself.
     */
    public String contentHash() {
        try (InputStream stream = openStream()) {
            return Sha256.hex(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
