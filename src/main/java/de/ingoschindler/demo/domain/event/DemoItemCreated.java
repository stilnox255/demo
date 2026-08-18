package de.ingoschindler.demo.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a new item has been committed.
 *
 * <p>A plain record with no framework annotations: the CDI event bus needs no
 * marker, and keeping it clean means a future outbox or message-broker adapter
 * can serialise the same type. Carries the data an observer needs rather than
 * the aggregate itself, so an observer cannot reach back into the write model.</p>
 */
public record DemoItemCreated(UUID id, String name, String ownerId, Instant occurredAt) {
}
