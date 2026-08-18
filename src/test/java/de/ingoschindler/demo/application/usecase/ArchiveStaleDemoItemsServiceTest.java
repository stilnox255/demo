package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The archive job's logic, with time as an input rather than a wait.
 *
 * <p>"Archive drafts older than 30 days" is untestable against the wall clock and
 * trivially testable against an injected one. That is the entire reason the use
 * case takes a {@link Clock}.</p>
 */
class ArchiveStaleDemoItemsServiceTest {

    static final Instant NOW = Instant.parse("2026-03-01T00:00:00Z");
    static final String OWNER = "owner-1";

    InMemoryDemoItemRepository repository;
    ArchiveStaleDemoItemsService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDemoItemRepository();
        service = new ArchiveStaleDemoItemsService();
        service.repository = repository;
        service.clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service.archiveAfter = Duration.ofDays(30);
        service.batchSize = 200;
    }

    @Test
    void archivesOnlyDraftsPastTheThreshold() {
        DemoItem old = save(DemoItem.create("old draft", "", OWNER, NOW.minus(Duration.ofDays(31))));
        DemoItem recent = save(DemoItem.create("recent draft", "", OWNER, NOW.minus(Duration.ofDays(29))));
        DemoItem active = save(DemoItem.create("old but active", "", OWNER, NOW.minus(Duration.ofDays(90)))
                .withStatus(DemoItemStatus.ACTIVE));

        assertEquals(1, service.archiveStale());

        assertEquals(DemoItemStatus.ARCHIVED, reload(old).status());
        assertEquals(DemoItemStatus.DRAFT, reload(recent).status());
        assertEquals(DemoItemStatus.ACTIVE, reload(active).status());
    }

    @Test
    void aRunIsBoundedByTheBatchSize() {
        service.batchSize = 2;
        for (int i = 0; i < 5; i++) {
            save(DemoItem.create("draft " + i, "", OWNER, NOW.minus(Duration.ofDays(31 + i))));
        }

        assertEquals(2, service.archiveStale(), "one run must never grow with the backlog");
    }

    @Test
    void aRunWithNothingToDoIsANoOp() {
        save(DemoItem.create("fresh", "", OWNER, NOW));

        assertEquals(0, service.archiveStale());
    }

    private DemoItem save(DemoItem item) {
        return repository.save(item);
    }

    private DemoItem reload(DemoItem item) {
        return repository.findByIdForOwner(item.id(), OWNER).orElseThrow();
    }
}
