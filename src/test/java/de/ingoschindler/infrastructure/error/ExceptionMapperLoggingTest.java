package de.ingoschindler.infrastructure.error;

import de.ingoschindler.demo.adapter.in.rest.DemoItemNotFoundExceptionMapper;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.infrastructure.web.errors.DatabaseExceptionMapper;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Guards the exception-mapper log-level convention (ADR-14): one record per
 * mapped exception, at the level its severity bucket prescribes.
 *
 * <p>The convention is only worth having if it is checked. Left to review, the
 * next mapper logs its 404 at WARN, and a month later the error log is mostly
 * client typos.</p>
 *
 * <p>Captures through a JUL handler on the root logger, because
 * {@code org.jboss.logging.Logger} routes into the installed
 * {@code org.jboss.logmanager.LogManager}, whose DEBUG/INFO/ERROR levels carry the
 * same numeric values as JUL's FINE/INFO/SEVERE — and {@link Level#equals}
 * compares by value, so the assertions below read naturally.</p>
 *
 * <p>Plain JUnit, no {@code @QuarkusTest}: a mapper is a function from exception
 * to response, and testing it does not need a container.</p>
 */
class ExceptionMapperLoggingTest {

    static final List<LogRecord> RECORDS = new ArrayList<>();

    Handler handler;
    Level previousRootLevel;

    @BeforeEach
    void installHandler() {
        RECORDS.clear();
        handler = new Handler() {
            @Override
            public void publish(LogRecord r) {
                RECORDS.add(r);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        var root = Logger.getLogger("");
        previousRootLevel = root.getLevel();
        root.addHandler(handler);
        root.setLevel(Level.ALL);
    }

    @AfterEach
    void removeHandler() {
        var root = Logger.getLogger("");
        root.removeHandler(handler);
        root.setLevel(previousRootLevel);
    }

    @Test
    void notFoundLogsAtDebugWithoutAThrowable() {
        var mapper = new DemoItemNotFoundExceptionMapper();

        try (var response = mapper.toResponse(new DemoItemNotFoundException(UUID.randomUUID()))) {
            assertEquals(404, response.getStatus());
        }

        var record = recordFor(DemoItemNotFoundExceptionMapper.class);
        assertEquals(Level.FINE, record.getLevel(), "the DEBUG bucket maps to JUL FINE");
        assertNull(record.getThrown(), "a 404 needs no stacktrace");
    }

    @Test
    void concurrentModificationLogsAtInfoWithoutAThrowable() {
        var mapper = new DatabaseExceptionMapper();

        try (var response = mapper.toResponse(new OptimisticLockException("row changed"))) {
            assertEquals(409, response.getStatus());
        }

        var record = recordFor(DatabaseExceptionMapper.class);
        assertEquals(Level.INFO, record.getLevel(), "a client-caused conflict belongs in the INFO bucket");
        assertNull(record.getThrown(), "a 409 needs no stacktrace");
    }

    @Test
    void unexpectedDatabaseFailureLogsAtErrorWithTheThrowable() {
        var mapper = new DatabaseExceptionMapper();
        var exception = new PersistenceException("connection reset");

        try (var response = mapper.toResponse(exception)) {
            assertEquals(500, response.getStatus());
        }

        var record = recordFor(DatabaseExceptionMapper.class);
        assertEquals(Level.SEVERE, record.getLevel(), "the ERROR bucket maps to JUL SEVERE");
        assertNotNull(record.getThrown(), "a 500 carries the throwable, which is the whole diagnostic value");
    }

    LogRecord recordFor(Class<?> mapperClass) {
        var matching = RECORDS.stream().filter(r -> mapperClass.getName().equals(r.getLoggerName())).toList();
        assertEquals(1, matching.size(), () -> "expected exactly one record from " + mapperClass.getSimpleName()
                + ", captured loggers: " + RECORDS.stream().map(LogRecord::getLoggerName).distinct().toList());
        return matching.getFirst();
    }
}
