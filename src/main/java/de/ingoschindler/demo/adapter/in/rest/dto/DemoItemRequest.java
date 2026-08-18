package de.ingoschindler.demo.adapter.in.rest.dto;

import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request body for creating and updating an item.
 *
 * <p>Constraints live here, at the boundary, and are checked by Bean Validation
 * before the use case runs (ADR-09) — one round trip, a machine-readable list of
 * field errors, and no hand-written {@code if (name == null)} chain. The domain
 * record still enforces the same invariants; that is not duplication but the
 * difference between a rejected request and an unrepresentable state.</p>
 *
 * <p>{@code ownerId} is absent on purpose: the server takes it from the token.</p>
 *
 * @param expectedVersion required on update, ignored on create
 */
@Schema(name = "DemoItemRequest", description = "Payload for creating or updating a demo item")
public record DemoItemRequest(

        @NotBlank @Size(max = DemoItem.NAME_MAX_LENGTH) @Schema(required = true, examples = "My first item") String name,

        @Size(max = DemoItem.DESCRIPTION_MAX_LENGTH) @Schema(examples = "What this item is for") String description,

        @Schema(description = "Target lifecycle state. Ignored on create, which always starts in DRAFT.") DemoItemStatus status,

        @Schema(description = "Version the client last read. Required on update for optimistic locking.") Long expectedVersion) {
}
