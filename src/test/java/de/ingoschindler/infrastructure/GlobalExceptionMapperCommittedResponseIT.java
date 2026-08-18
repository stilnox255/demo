package de.ingoschindler.infrastructure;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-18: {@link GlobalExceptionMapper}'s "response already committed" guard. Vert.x's body-limit
 * handler can flush a 413 itself before RESTEasy finishes reading an oversized request body; the
 * resulting {@link IOException} then reaches this mapper's catch-all branch on a connection that
 * is already closed. Writing a second (500) response there throws {@code IllegalStateException}
 * deep in RESTEasy's writer pipeline — log noise, since the client already has the correct 413.
 *
 * <p>Same JUL-log-capture technique as {@code ExceptionMapperLoggingIT} (direct mapper invocation,
 * no {@code @QuarkusTest}), placed in {@code de.ingoschindler.infrastructure} rather than
 * {@code infrastructure.error} so the test can set the mapper's package-private
 * {@code currentVertxRequest} field directly instead of going through CDI.
 */
class GlobalExceptionMapperCommittedResponseIT {

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
    void genuineUnhandledExceptionLogsAtErrorAndKeepsProblemJsonBody() {
        var mapper = new GlobalExceptionMapper();
        mapper.currentVertxRequest = currentVertxRequestWithResponseEnded(false);
        var exception = new IllegalStateException("boom");

        try (var response = mapper.toResponse(exception)) {
            assertEquals(500, response.getStatus(), "genuine 500 still gets a body, not a silent no-body response");
            assertTrue(response.hasEntity(), "genuine 500 still carries a problem+json entity");
        }

        var record = recordFor();
        assertEquals(Level.SEVERE, record.getLevel(), "genuine unhandled exceptions still log at ERROR");
        assertEquals(exception, record.getThrown(), "genuine unhandled exceptions still attach the throwable");
    }

    @Test
    void alreadyCommittedResponseSkipsSecondWriteAndLogsAtDebug() {
        var mapper = new GlobalExceptionMapper();
        mapper.currentVertxRequest = currentVertxRequestWithResponseEnded(true);
        var exception = new IOException("Request too large");

        try (var response = mapper.toResponse(exception)) {
            assertFalse(response.hasEntity(),
                    "no entity means RESTEasy never opens a second output stream on the already-closed connection");
        }

        var record = recordFor();
        assertEquals(Level.FINE, record.getLevel(), "already-committed case is log noise, not an error: DEBUG bucket");
    }

    private static CurrentVertxRequest currentVertxRequestWithResponseEnded(boolean ended) {
        HttpServerResponse response = mock(HttpServerResponse.class);
        when(response.ended()).thenReturn(ended);
        RoutingContext routingContext = mock(RoutingContext.class);
        when(routingContext.response()).thenReturn(response);
        CurrentVertxRequest currentVertxRequest = mock(CurrentVertxRequest.class);
        when(currentVertxRequest.getCurrent()).thenReturn(routingContext);
        return currentVertxRequest;
    }

    LogRecord recordFor() {
        return RECORDS.stream().filter(r -> GlobalExceptionMapper.class.getName().equals(r.getLoggerName())).findFirst()
                .orElseThrow(
                        () -> new AssertionError("no log record emitted by GlobalExceptionMapper; captured loggers: "
                                + RECORDS.stream().map(LogRecord::getLoggerName).distinct().toList()));
    }
}
