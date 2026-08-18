package de.ingoschindler.demo.application.port.in;

import de.ingoschindler.demo.domain.DemoItem;

/** Shared single-item result envelope for the write use cases. */
public record DemoItemResult(DemoItem item) {
}
