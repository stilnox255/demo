package de.ingoschindler.demo.application.port.in;

import de.ingoschindler.demo.domain.DemoItem;

/**
 * Output of {@link CreateDemoItemUseCase}. Returns the domain type rather than a
 * DTO — the REST layer owns its own wire format and maps in
 * {@code adapter.in.rest.dto}.
 */
public record CreateDemoItemResult(DemoItem item) {
}
