package de.ingoschindler.infrastructure.logging;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two-sink logging contract (ADR-23): the console stays plain text in
 * every profile so {@code docker compose logs} is readable directly, and
 * structured JSON goes out over syslog to the log collector in production only.
 *
 * <p>A config-shape check, not a runtime one. Each of these values was picked for
 * a reason that is invisible from the key itself, so the reason lives in the
 * assertion message and the value cannot drift back silently.</p>
 *
 * <p>Reads {@code src/main/resources/application.properties} from the filesystem
 * rather than the classpath: the classpath here is shadowed by
 * {@code src/test/resources/application.properties}, which would defeat the
 * purpose. Gradle runs from the project root.</p>
 */
class JsonConsoleLoggingConfigTest {

    private static final Path APPLICATION_PROPERTIES = Path.of("src/main/resources/application.properties");

    private static Properties applicationProperties;

    @BeforeAll
    static void loadApplicationProperties() throws IOException {
        assertTrue(Files.isRegularFile(APPLICATION_PROPERTIES),
                "expected to find " + APPLICATION_PROPERTIES.toAbsolutePath() + " — run from the project root");
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(APPLICATION_PROPERTIES)) {
            properties.load(stream);
        }
        applicationProperties = properties;
    }

    @Test
    void consoleStaysPlainTextForEveryProfile() {
        assertEquals("false", applicationProperties.getProperty("quarkus.log.console.json.enabled"),
                "the console must stay human-readable in every profile — `docker compose logs` is read by eye,"
                        + " not through a JSON parser");
    }

    @Test
    void consoleFormatCarriesTheCorrelationIds() {
        String format = applicationProperties.getProperty("quarkus.log.console.format");
        assertTrue(format.contains("%X{userId}"), "user correlation belongs on every line, not on a subset");
        assertTrue(format.contains("%X{traceId}"), "a line without a trace id cannot be joined to a trace");
        assertTrue(format.contains("%X{spanId}"), "the span id completes the correlation triple");
    }

    @Test
    void productionShipsStructuredJsonOverSyslog() {
        assertEquals("true", applicationProperties.getProperty("%prod.quarkus.log.syslog.enabled"),
                "prod ships to the log collector; dev and test deliberately do not");
        assertEquals("true", applicationProperties.getProperty("%prod.quarkus.log.syslog.json.enabled"),
                "the shipped copy is JSON — that is the one a query engine reads");
        assertEquals("detailed",
                applicationProperties.getProperty("%prod.quarkus.log.syslog.json.exception-output-type"),
                "a truncated stacktrace is the one field nobody can reconstruct afterwards");
        assertEquals("true", applicationProperties.getProperty("%prod.quarkus.log.syslog.json.print-details"),
                "caller details are what make a shipped line traceable back to code");
    }

    @Test
    void theCollectorEndpointIsConfigurablePerEnvironment() {
        String endpoint = applicationProperties.getProperty("%prod.quarkus.log.syslog.endpoint");
        assertTrue(endpoint != null && endpoint.startsWith("${"),
                "the collector address differs per environment, so it comes from the environment (ADR-24);"
                        + " a literal here would need a rebuild to move");
    }

    @Test
    void logShippingIsBestEffortNotBlocking() {
        assertEquals("udp", applicationProperties.getProperty("%prod.quarkus.log.syslog.protocol"),
                "UDP on purpose: log shipping must never apply backpressure to a request thread just because"
                        + " the collector is briefly down");
    }

    @Test
    void otelPropagatorsArePinned() {
        assertEquals("tracecontext,baggage", applicationProperties.getProperty("quarkus.otel.propagators"),
                "propagation is part of the contract with callers, not a default to inherit silently");
    }

    @Test
    void environmentsWithoutACollectorExportNoTraces() {
        for (String profile : new String[]{"dev", "test"}) {
            assertEquals("none", applicationProperties.getProperty("%" + profile + ".quarkus.otel.traces.exporter"),
                    profile + " has no OTLP collector, and an exporter without one retries in the background"
                            + " forever while logging about it");
        }
    }
}
