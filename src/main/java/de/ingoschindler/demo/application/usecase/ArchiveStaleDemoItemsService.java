package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.ArchiveStaleDemoItemsUseCase;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Archives drafts nobody finished.
 *
 * <p>Bounded twice over: the repository returns at most {@code batch-size} rows,
 * and {@code @Timeout} caps the whole run. Background work that can grow without
 * limit competes with request traffic for the same connection pool, and the first
 * time anyone notices is when the pool runs dry during a spike.</p>
 *
 * <p>When the timeout fires the run is interrupted and the transaction rolls
 * back, so the next tick retries the same batch rather than resuming a half-done
 * one. That is the intended behaviour: the work is idempotent.</p>
 *
 * <p>One transaction for the batch, not one per row: the batch is small and
 * bounded, so partial progress buys nothing over a clean retry on the next tick.
 * Were the batch large, the trade flips — then per-row transactions keep a single
 * bad row from discarding the work done before it.</p>
 */
@ApplicationScoped
public class ArchiveStaleDemoItemsService implements ArchiveStaleDemoItemsUseCase {

    @Inject
    DemoItemRepository repository;

    @Inject
    Clock clock;

    @ConfigProperty(name = "starter.demo.archive.after")
    Duration archiveAfter;

    @ConfigProperty(name = "starter.demo.archive.batch-size", defaultValue = "200")
    int batchSize;

    @Override
    @Transactional
    @Timeout
    public int archiveStale() {
        List<DemoItem> stale = repository.findStaleDrafts(clock.instant().minus(archiveAfter), batchSize);
        stale.forEach(item -> repository.save(item.withStatus(DemoItemStatus.ARCHIVED)));
        return stale.size();
    }
}
