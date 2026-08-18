package de.ingoschindler.demo.application.port.in;

/**
 * Input for {@link CreateDemoItemUseCase}.
 *
 * <p>{@code ownerId} is not a client-supplied field: the inbound adapter derives
 * it from the authenticated principal. A command that accepted an owner from the
 * request body would let a caller create items in someone else's name.</p>
 */
public record CreateDemoItemCommand(String name, String description, String ownerId) {
}
