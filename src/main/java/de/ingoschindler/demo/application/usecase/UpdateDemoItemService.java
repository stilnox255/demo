package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.DemoItemResult;
import de.ingoschindler.demo.application.port.in.UpdateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.UpdateDemoItemUseCase;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Applies a client-supplied change to an existing item.
 *
 * <p>Reads with the caller's {@code ownerId}, then replays the caller's
 * {@code expectedVersion} onto the aggregate before saving. If another request
 * has written in between, the store rejects the update and the mapper turns that
 * into 409 — the caller re-reads and decides. Silently overwriting would be the
 * alternative, and it loses data without anyone noticing.</p>
 */
@ApplicationScoped
public class UpdateDemoItemService implements UpdateDemoItemUseCase {

    @Inject
    DemoItemRepository repository;

    @Override
    @Transactional
    public DemoItemResult update(UpdateDemoItemCommand command) {
        DemoItem current = repository.findByIdForOwner(command.id(), command.ownerId())
                .orElseThrow(() -> new DemoItemNotFoundException(command.id()));

        DemoItem updated = current.withDetails(command.name(), command.description()).withStatus(command.status())
                .atVersion(command.expectedVersion());

        return new DemoItemResult(repository.save(updated));
    }
}
