package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.CreateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.CreateDemoItemResult;
import de.ingoschindler.demo.application.port.in.CreateDemoItemUseCase;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.event.DemoItemCreated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Clock;

/**
 * Creates an item and announces it.
 *
 * <p>{@code @Transactional} sits here, on the use case, and nowhere below it
 * (ADR-04). A helper or a repository that opens its own transaction makes the
 * boundary invisible to the only class that knows what "one unit of work" means
 * for this operation.</p>
 *
 * <p>The event fires inside the transaction but is observed
 * {@code AFTER_SUCCESS}, so an observer never reacts to a write that later rolls
 * back — see {@code adapter.in.messaging}.</p>
 */
@ApplicationScoped
public class CreateDemoItemService implements CreateDemoItemUseCase {

    @Inject
    DemoItemRepository repository;

    @Inject
    Event<DemoItemCreated> created;

    @Inject
    Clock clock;

    @Override
    @Transactional
    public CreateDemoItemResult create(CreateDemoItemCommand command) {
        DemoItem saved = repository
                .save(DemoItem.create(command.name(), command.description(), command.ownerId(), clock.instant()));
        created.fire(new DemoItemCreated(saved.id(), saved.name(), saved.ownerId(), clock.instant()));
        return new CreateDemoItemResult(saved);
    }
}
