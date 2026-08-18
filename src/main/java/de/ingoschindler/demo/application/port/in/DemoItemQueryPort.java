package de.ingoschindler.demo.application.port.in;

import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.kernel.pagination.Page;
import de.ingoschindler.kernel.pagination.PageRequest;

import java.util.List;
import java.util.UUID;

/**
 * Published in-port for the read side.
 *
 * <p>A multi-method read facade rather than three {@code *UseCase} interfaces:
 * there is no single action being executed and no {@code Command}/{@code Result}
 * envelope worth inventing, so it takes the {@code QueryPort}/{@code Query}
 * naming pair instead of being bent into the command shape (ADR-01).</p>
 */
public interface DemoItemQueryPort {

    DemoItem byIdForOwner(UUID id, String ownerId);

    Page<DemoItem> pageForOwner(PageRequest request, String ownerId);

    /** All items of one owner, newest first. Backs the cached summary endpoint. */
    List<DemoItem> allForOwner(String ownerId);
}
