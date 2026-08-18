package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.CreateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.UpdateDemoItemCommand;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.demo.domain.DemoItemStatus;
import de.ingoschindler.demo.domain.event.DemoItemCreated;
import jakarta.enterprise.event.Event;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The write use cases, driven directly with a fake repository and a fixed clock.
 *
 * <p>No {@code @QuarkusTest}: these classes take their collaborators through
 * fields, so a test can supply them without the container. That is the design
 * signal the layering exists for — if this had to boot Quarkus to assert an
 * ownership rule, the rule would be in the wrong place.</p>
 */
class DemoItemWriteUseCasesTest {

    static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    static final String OWNER = "owner-1";

    InMemoryDemoItemRepository repository;

    /**
     * The CDI event dispatcher is mocked rather than faked: it is a framework
     * interface at a boundary with six methods, five of which these cases never
     * touch. Mocking at a boundary is fine; mocking the subject under scrutiny is
     * not.
     */
    @SuppressWarnings("unchecked")
    Event<DemoItemCreated> events = mock(Event.class);

    CreateDemoItemService create;
    UpdateDemoItemService update;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDemoItemRepository();

        create = new CreateDemoItemService();
        create.repository = repository;
        create.created = events;
        create.clock = Clock.fixed(NOW, ZoneOffset.UTC);

        update = new UpdateDemoItemService();
        update.repository = repository;
    }

    @Test
    void createPersistsAndAnnouncesTheItem() {
        DemoItem item = create.create(new CreateDemoItemCommand("First", "why", OWNER)).item();

        assertEquals(DemoItemStatus.DRAFT, item.status());
        assertTrue(repository.findByIdForOwner(item.id(), OWNER).isPresent());

        var captor = ArgumentCaptor.forClass(DemoItemCreated.class);
        verify(events).fire(captor.capture());
        DemoItemCreated event = captor.getValue();
        assertEquals(item.id(), event.id());
        assertEquals(OWNER, event.ownerId());
        assertEquals(NOW, event.occurredAt(), "the event is timestamped from the injected clock, not the wall clock");
    }

    @Test
    void updateAppliesTheChangeAndBumpsTheVersion() {
        DemoItem created = create.create(new CreateDemoItemCommand("First", "", OWNER)).item();

        DemoItem updated = update.update(new UpdateDemoItemCommand(created.id(), OWNER, "Renamed", "now with a why",
                DemoItemStatus.ACTIVE, created.version())).item();

        assertEquals("Renamed", updated.name());
        assertEquals(DemoItemStatus.ACTIVE, updated.status());
        assertEquals(created.version() + 1, updated.version());
    }

    @Test
    void updateWithAStaleVersionIsRejected() {
        DemoItem created = create.create(new CreateDemoItemCommand("First", "", OWNER)).item();
        long staleVersion = created.version();
        update.update(
                new UpdateDemoItemCommand(created.id(), OWNER, "Winner", "", DemoItemStatus.ACTIVE, staleVersion));

        // Second writer still holds the version it read before the first write.
        assertThrows(OptimisticLockException.class, () -> update.update(
                new UpdateDemoItemCommand(created.id(), OWNER, "Loser", "", DemoItemStatus.ACTIVE, staleVersion)));

        assertEquals("Winner", repository.findByIdForOwner(created.id(), OWNER).orElseThrow().name());
    }

    @Test
    void anotherOwnerGetsNotFoundRatherThanForbidden() {
        DemoItem created = create.create(new CreateDemoItemCommand("First", "", OWNER)).item();

        assertThrows(DemoItemNotFoundException.class, () -> update.update(new UpdateDemoItemCommand(created.id(),
                "someone-else", "Hijacked", "", DemoItemStatus.ACTIVE, created.version())));
    }

    @Test
    void updatingAnUnknownIdIsNotFound() {
        assertThrows(DemoItemNotFoundException.class, () -> update
                .update(new UpdateDemoItemCommand(UUID.randomUUID(), OWNER, "Ghost", "", DemoItemStatus.ACTIVE, 0L)));
    }

}
