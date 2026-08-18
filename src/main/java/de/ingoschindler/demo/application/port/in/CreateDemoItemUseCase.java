package de.ingoschindler.demo.application.port.in;

/**
 * Published in-port: create a new item.
 *
 * <p>Every use case publishes an interface like this one, whether or not another
 * component currently calls it (ADR-01). Inbound adapters depend on the
 * interface, never on the implementing {@code *Service} class — that is what
 * keeps a REST resource from growing a compile-time dependency on application
 * internals.</p>
 */
public interface CreateDemoItemUseCase {

    CreateDemoItemResult create(CreateDemoItemCommand command);
}
