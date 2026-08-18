package de.ingoschindler.demo.application.port.in;

/**
 * Published in-port: archive drafts that have gone stale.
 *
 * <p>Driven by a scheduled job, but the job holds no logic of its own — it is an
 * inbound adapter like any REST resource. That split is what makes the behaviour
 * testable without waiting for a cron tick, and re-runnable by hand during an
 * incident.</p>
 */
public interface ArchiveStaleDemoItemsUseCase {

    /**
     * @return how many items were archived in this run, for the job to log and
     *         count as a metric
     */
    int archiveStale();
}
