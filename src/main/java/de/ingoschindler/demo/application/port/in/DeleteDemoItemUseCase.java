package de.ingoschindler.demo.application.port.in;

import java.util.UUID;

/**
 * Published in-port: delete an item and its attachment.
 *
 * <p>No {@code Command} record for a two-argument call — a record whose only job
 * is to wrap an id and the caller's identity adds a file and removes nothing.</p>
 */
public interface DeleteDemoItemUseCase {

    void delete(UUID id, String ownerId);
}
