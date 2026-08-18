package de.ingoschindler.demo.application.port.in;

import java.util.UUID;

/**
 * Published in-port: read the attachment bytes of an item.
 *
 * <p>Authorization for this one path is the signed token checked by the inbound
 * adapter, not the OIDC principal — a browser cannot put an
 * {@code Authorization} header on an {@code <img src>} (ADR-19). The use case
 * therefore takes no {@code ownerId}: the token already binds the request to
 * exactly one item id.</p>
 */
public interface DownloadDemoItemAttachmentUseCase {

    DownloadDemoItemAttachmentResult download(UUID id);
}
