package de.ingoschindler.demo.adapter.in.scheduled;

import de.ingoschindler.demo.application.port.in.ArchiveStaleDemoItemsUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Timer for {@link ArchiveStaleDemoItemsUseCase}. Holds no logic — an inbound
 * adapter whose transport happens to be a clock.
 *
 * <p>The schedule is configured, not hardcoded, and its default is deliberately
 * <em>not</em> on a round boundary: everything set to {@code 0 0 * * *} across a
 * fleet hits the database in the same second. Off-boundary minutes and seconds
 * cost nothing and spread the load.</p>
 *
 * <p>{@code ConcurrentExecution.SKIP} because two runs would fight over the same
 * rows. If a run is still going when the next tick arrives, the answer is to skip
 * it and look at why it is slow, not to pile on.</p>
 *
 * <p>Failures are caught and logged rather than propagated: an escaping exception
 * would be logged by the scheduler with no context of its own, and the run count
 * is the signal worth having.</p>
 */
@ApplicationScoped
public class ArchiveStaleDemoItemsJob {

    private static final Logger LOGGER = Logger.getLogger(ArchiveStaleDemoItemsJob.class);

    @Inject
    ArchiveStaleDemoItemsUseCase archiveStaleDemoItems;

    @Scheduled(cron = "{starter.demo.archive.cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            int archived = archiveStaleDemoItems.archiveStale();
            LOGGER.infof("archive_stale_demo_items outcome=success archived=%d", archived);
        } catch (RuntimeException e) {
            LOGGER.errorf(e, "archive_stale_demo_items outcome=failure");
        }
    }
}
