package de.ingoschindler.demo.application.port.in;

import de.ingoschindler.kernel.upload.UploadedFile;

import java.util.UUID;

/**
 * Input for {@link AttachFileToDemoItemUseCase}.
 *
 * <p>Carries an {@link UploadedFile} and not a JAX-RS {@code FileUpload}: the
 * application layer must stay drivable from a scheduled job or a test without an
 * HTTP request to fake.</p>
 */
public record AttachFileToDemoItemCommand(UUID id, String ownerId, UploadedFile file) {
}
