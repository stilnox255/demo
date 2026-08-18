package de.ingoschindler.demo.application.port.in;

import java.io.InputStream;

/**
 * Output of {@link DownloadDemoItemAttachmentUseCase}.
 *
 * <p>The stream is open and unread. The inbound adapter owns closing it, which
 * it does by handing it to JAX-RS as the response entity — buffering the bytes
 * here to "own" them would put the whole file on the heap for no gain.</p>
 */
public record DownloadDemoItemAttachmentResult(InputStream content, String fileName, String contentType, long size) {
}
