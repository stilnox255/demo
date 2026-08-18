package de.ingoschindler.infrastructure.logging;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LoggingFilterTest {

    private CapturingHandler handler;

    @BeforeEach
    void installHandler() {
        handler = new CapturingHandler();
        Logger.getLogger("").addHandler(handler);
    }

    @AfterEach
    void removeHandler() {
        Logger.getLogger("").removeHandler(handler);
    }

    @Test
    void logs_request_entry_and_completion_with_method_path_status_and_duration() {
        given().when().get("/test/logging/ping").then().statusCode(200);

        List<LogRecord> records = handler.snapshot();
        assertTrue(
                records.stream()
                        .anyMatch(r -> contains(r, "request_received") && contains(r, "method=GET")
                                && contains(r, "/test/logging/ping")),
                "expected request_received with method and path");
        assertTrue(
                records.stream()
                        .anyMatch(r -> contains(r, "request_completed") && contains(r, "status=200")
                                && contains(r, "duration_ms=")),
                "expected request_completed with status=200 and duration_ms");
    }

    @Test
    void does_not_log_authorization_header_contents() {
        String secretToken = "super-secret-bearer-token-xyz";
        given().header("Authorization", "Bearer " + secretToken).when().get("/test/logging/ping").then()
                .statusCode(200);

        List<LogRecord> records = handler.snapshot();
        assertFalse(records.stream().anyMatch(r -> contains(r, secretToken)),
                "log output must not contain the bearer token");
    }

    private static boolean contains(LogRecord record, String needle) {
        if (record == null) {
            return false;
        }
        String message = record.getMessage();
        if (message != null && message.contains(needle)) {
            return true;
        }
        Object[] params = record.getParameters();
        if (params != null) {
            for (Object param : params) {
                if (param != null && param.toString().contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    static final class CapturingHandler extends Handler {

        private final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            records.add(logRecord);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        public void setFormatter(Formatter newFormatter) {
            super.setFormatter(newFormatter);
        }

        public List<LogRecord> snapshot() {
            return List.copyOf(records);
        }
    }

    @ApplicationScoped
    @Path("/test/logging")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TestPingResource {

        @GET
        @Path("/ping")
        public String ping() {
            return "pong";
        }
    }
}
