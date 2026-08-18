package de.ingoschindler.demo.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The domain rules, tested with plain values.
 *
 * <p>No container, no database, no mocks — that is the payoff of keeping the
 * aggregate a record (ADR-01). If this test ever needs a {@code @QuarkusTest}, the
 * layering has been broken somewhere above it.</p>
 */
class DemoItemTest {

    static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void createStartsAsADraftWithAnIdentityOfItsOwn() {
        var item = DemoItem.create("My item", "why", "owner-1", NOW);

        assertEquals(DemoItemStatus.DRAFT, item.status());
        assertEquals(NOW, item.createdAt());
        assertEquals(0L, item.version(), "an unpersisted item starts at version 0");
        assertTrue(item.findAttachment().isEmpty());
        assertFalse(item.id().toString().isBlank());
    }

    @Test
    void nameIsTrimmedAndRequired() {
        assertEquals("Trimmed", DemoItem.create("  Trimmed  ", "", "owner-1", NOW).name());

        assertThrows(IllegalArgumentException.class, () -> DemoItem.create("   ", "", "owner-1", NOW));
        assertThrows(IllegalArgumentException.class, () -> DemoItem.create(null, "", "owner-1", NOW));
    }

    @Test
    void nameAndDescriptionAreLengthBounded() {
        assertThrows(IllegalArgumentException.class,
                () -> DemoItem.create("x".repeat(DemoItem.NAME_MAX_LENGTH + 1), "", "owner-1", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> DemoItem.create("ok", "x".repeat(DemoItem.DESCRIPTION_MAX_LENGTH + 1), "owner-1", NOW));
    }

    @Test
    void ownerIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> DemoItem.create("ok", "", " ", NOW));
    }

    @Test
    void missingDescriptionBecomesEmptyRatherThanNull() {
        assertEquals("", DemoItem.create("ok", null, "owner-1", NOW).description());
    }

    @Test
    void transitionsReturnANewInstance() {
        var draft = DemoItem.create("ok", "", "owner-1", NOW);

        var active = draft.withStatus(DemoItemStatus.ACTIVE);

        assertNotSame(draft, active);
        assertEquals(DemoItemStatus.DRAFT, draft.status(), "the original is untouched");
        assertEquals(DemoItemStatus.ACTIVE, active.status());
        assertEquals(draft.id(), active.id());
    }

    @Test
    void archivedIsTerminal() {
        var archived = DemoItem.create("ok", "", "owner-1", NOW).withStatus(DemoItemStatus.ARCHIVED);

        assertThrows(IllegalArgumentException.class, () -> archived.withStatus(DemoItemStatus.ACTIVE));
        // Re-archiving is a no-op rather than an error: the caller's intent is
        // already satisfied, and failing here would make the archive job's retry
        // path fail on rows it already handled.
        assertEquals(DemoItemStatus.ARCHIVED, archived.withStatus(DemoItemStatus.ARCHIVED).status());
    }

    @Test
    void staleMeansDraftAndOlderThanTheThreshold() {
        var draft = DemoItem.create("ok", "", "owner-1", NOW);

        assertTrue(draft.isStaleDraft(NOW.plus(1, ChronoUnit.DAYS)));
        assertFalse(draft.isStaleDraft(NOW), "the boundary is exclusive: created at the threshold is not yet stale");
        assertFalse(draft.withStatus(DemoItemStatus.ACTIVE).isStaleDraft(NOW.plus(1, ChronoUnit.DAYS)),
                "only drafts go stale");
    }

    @Test
    void ownershipIsAnExactMatch() {
        var item = DemoItem.create("ok", "", "owner-1", NOW);

        assertTrue(item.isOwnedBy("owner-1"));
        assertFalse(item.isOwnedBy("owner-2"));
        assertFalse(item.isOwnedBy("OWNER-1"));
    }
}
