package de.ingoschindler.demo.adapter.in.rest.dto;

import de.ingoschindler.demo.domain.DemoItemStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Reduced projection served by the cached summary endpoint.
 *
 * <p>Separate from {@link DemoItemResponse} because it is the type that gets
 * serialised into the cache. Anything in a cached value is a wire format with a
 * deployed-version skew problem attached, so it stays as small as it can be —
 * and it deliberately carries no signed download URL, which would expire inside
 * the cache entry.</p>
 */
@Schema(name = "DemoItemSummary", description = "Reduced demo item projection for polling clients")
public record DemoItemSummary(UUID id, String name, DemoItemStatus status, boolean hasAttachment) {
}
