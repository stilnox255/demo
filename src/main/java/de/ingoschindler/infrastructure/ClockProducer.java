package de.ingoschindler.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

/**
 * Makes the clock injectable.
 *
 * <p>Use cases take a {@link Clock} instead of calling {@code Instant.now()},
 * which is what lets a test about "items older than 30 days" run in milliseconds
 * with a fixed clock instead of not being written at all. Deliberately UTC: the
 * host timezone is not an input anything should depend on.</p>
 */
@ApplicationScoped
public class ClockProducer {

    @Produces
    @ApplicationScoped
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
