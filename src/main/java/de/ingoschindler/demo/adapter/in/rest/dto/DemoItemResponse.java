package de.ingoschindler.demo.adapter.in.rest.dto;

import de.ingoschindler.demo.domain.DemoItemStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Full representation of one item.
 *
 * <p>A typed record, not a {@code Map<String, Object>}: the map version compiles
 * against any typo, documents nothing in OpenAPI and turns every field rename
 * into a runtime surprise (ADR-10).</p>
 *
 * @param attachment      null when nothing is attached
 * @param version         echoed back so the client can send it as
 *                        {@code expectedVersion} on the next update
 */
@Schema(name = "DemoItem", description = "A demo item")
public record DemoItemResponse(UUID id, String name, String description, DemoItemStatus status, Instant createdAt,
        long version, Attachment attachment) {

    /**
     * @param downloadUrl absolute, signed and short-lived — mint a fresh one by
     *                    re-reading the item rather than storing this
     */
    @Schema(name = "DemoItemAttachment")
    public record Attachment(String fileName, String contentType, long size, String downloadUrl) {
    }
}
