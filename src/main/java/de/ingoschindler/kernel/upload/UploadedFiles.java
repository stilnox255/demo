package de.ingoschindler.kernel.upload;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * Conversion helpers from transport-specific upload types to {@link UploadedFile}.
 *
 * <p>Lives in the kernel so every inbound REST adapter
 * ({@code {bc}.adapter.in.rest}) can build an {@link UploadedFile} without
 * re-implementing the wiring. This is the one class allowed to mention
 * {@link FileUpload}: it is the seam that keeps JAX-RS out of every port
 * signature behind it. The supplier opens the underlying file lazily so the
 * bytes can be read more than once if the use case needs to (hash, then
 * upload).
 */
public final class UploadedFiles {

    UploadedFiles() {
    }

    public static UploadedFile from(FileUpload upload) {
        return new UploadedFile(upload.fileName(), upload.contentType(), upload.size(), () -> openTempFile(upload));
    }

    static InputStream openTempFile(FileUpload upload) {
        try {
            return Files.newInputStream(upload.uploadedFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
