package simulations;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.header;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Capacity test for the demo resource (ADR-43).
 *
 * <p>Three read/write mixes, because they saturate different things and the
 * interesting failures are at the seams:</p>
 *
 * <ul>
 * <li><b>pollers</b> — repeat {@code GET /summary} with the {@code ETag} they got
 * last time. Should be almost free: a cache hit and a 304 with no body. If this
 * scenario shows database load, either the cache is not being hit or the
 * invalidation is firing on every request.</li>
 * <li><b>readers</b> — paginated list requests, which deliberately bypass the
 * cache. This is where the connection pool shows up.</li>
 * <li><b>writers</b> — create and update, each one invalidating the pollers'
 * cache entry. The point of running all three at once: a write rate that keeps
 * the cache permanently cold turns the pollers into readers, and no
 * single-scenario run would ever reveal that.</li>
 * </ul>
 *
 * <p>Assertions rather than a report to read by eye: a capacity test that only
 * produces graphs gets run once. These fail the task, so they can go in a
 * pipeline.</p>
 *
 * <p>Run against a deployed environment — never against production without
 * agreeing it first:</p>
 *
 * <pre>
 * ./gradlew :load-test:runSimulation \
 *   -Dgatling.simulationClass=simulations.DemoItemsSimulation \
 *   -DbaseUrl=https://app.example.com \
 *   -DbearerToken="$(...)" \
 *   -DusersPerSec=5 -DdurationSeconds=120
 * </pre>
 */
public class DemoItemsSimulation extends Simulation {

    static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    /**
     * An access token for a user with the {@code user} role. Obtain it from
     * Keycloak with a direct grant; the simulation does not perform the OIDC
     * dance itself, because measuring the identity provider is a different test.
     */
    static final String BEARER_TOKEN = System.getProperty("bearerToken", "");

    static final double USERS_PER_SEC = Double.parseDouble(System.getProperty("usersPerSec", "5"));
    static final int DURATION_SECONDS = Integer.getInteger("durationSeconds", 60);

    HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL).acceptHeader("application/json")
            .contentTypeHeader("application/json").acceptEncodingHeader("gzip")
            .authorizationHeader("Bearer " + BEARER_TOKEN).userAgentHeader("starter-loadtest")
            // shareConnections, because a per-user connection pool would measure
            // TLS handshakes rather than the application.
            .shareConnections();

    // --- Poll with a conditional GET -----------------------------------------

    ChainBuilder pollSummary = exec(
            http("GET /summary (first)").get("/api/demo-items/summary").check(status().is(200))
                    .check(header("ETag").saveAs("etag")))
            .pause(Duration.ofSeconds(1))
            .exec(http("GET /summary (If-None-Match)").get("/api/demo-items/summary")
                    .header("If-None-Match", "#{etag}")
                    // 304 is the success case here, and 200 is a legitimate answer
                    // after a concurrent write — so both are accepted and the
                    // assertion below covers the rest.
                    .check(status().in(200, 304)));

    // --- Paginated read ------------------------------------------------------

    ChainBuilder listPage = exec(http("GET /demo-items?page").get("/api/demo-items?page=1&pageSize=25")
            .check(status().is(200)).check(jmesPath("meta.pageSize").isEL("25")));

    // --- Write, which invalidates the pollers' entry -------------------------

    ChainBuilder createAndUpdate = exec(
            http("POST /demo-items").post("/api/demo-items")
                    .body(StringBody("{\"name\":\"load-#{randomUuid()}\",\"description\":\"generated\"}"))
                    .check(status().is(201)).check(jmesPath("id").saveAs("itemId"))
                    .check(jmesPath("version").saveAs("version")))
            .pause(Duration.ofMillis(500))
            .exec(http("PUT /demo-items/{id}").put("/api/demo-items/#{itemId}")
                    .body(StringBody(
                            "{\"name\":\"load-updated\",\"description\":\"\",\"status\":\"ACTIVE\",\"expectedVersion\":#{version}}"))
                    .check(status().is(200)))
            .exec(http("DELETE /demo-items/{id}").delete("/api/demo-items/#{itemId}").check(status().is(204)));

    ScenarioBuilder pollers = scenario("pollers").exec(pollSummary);
    ScenarioBuilder readers = scenario("readers").exec(listPage);
    ScenarioBuilder writers = scenario("writers").exec(createAndUpdate);

    {
        setUp(
                // 60/30/10 read-heavy, which is the shape of the traffic this
                // endpoint is designed for. A 50/50 mix would be a different test
                // and should be its own simulation rather than a knob here.
                pollers.injectOpen(constantUsersPerSec(USERS_PER_SEC * 0.6).during(Duration.ofSeconds(DURATION_SECONDS))),
                readers.injectOpen(constantUsersPerSec(USERS_PER_SEC * 0.3).during(Duration.ofSeconds(DURATION_SECONDS))),
                writers.injectOpen(constantUsersPerSec(USERS_PER_SEC * 0.1).during(Duration.ofSeconds(DURATION_SECONDS))))
                .protocols(httpProtocol)
                .assertions(
                        // No failed requests at all. A capacity test that tolerates
                        // errors is measuring how fast the system can fail.
                        global().failedRequests().count().is(0L),
                        global().responseTime().percentile3().lt(1000),
                        // The conditional GET is the cheapest request in the system
                        // and has to stay that way; this is the assertion that
                        // catches a broken cache.
                        details("GET /summary (If-None-Match)").responseTime().percentile3().lt(200));
    }
}
