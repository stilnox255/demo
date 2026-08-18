package de.ingoschindler.demo.application.port.in;

import de.ingoschindler.demo.domain.DemoItemStatus;

import java.util.UUID;

/**
 * Input for {@link UpdateDemoItemUseCase}.
 *
 * @param expectedVersion the {@code version} the caller read. Mandatory: an
 *                        update without it is a last-write-wins update, and the
 *                        loser never finds out.
 */
public record UpdateDemoItemCommand(UUID id, String ownerId, String name, String description, DemoItemStatus status,
        long expectedVersion) {
}
