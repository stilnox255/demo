package de.ingoschindler.infrastructure.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Pins what the two probes are actually made of, so a future edit cannot quietly drop a dependency
 * from readiness or reintroduce a duplicate.
 *
 * <p>No hand-written database check: quarkus-agroal already registers
 * {@code DataSourceHealthCheck} ("Database connections health check") as {@code @Readiness}, so
 * adding one probes the database twice per call. Nor a hand-written liveness check that returns UP
 * unconditionally — that is exactly what SmallRye Health reports for a liveness endpoint with no
 * checks registered.
 *
 * <p>S3 has no such built-in ({@code quarkus-amazon-s3} ships no health check at all), so
 * {@link S3HealthCheck} stays and must remain present here. The cache is deliberately absent from
 * readiness (ADR-21, ADR-41): it degrades, it does not stop the instance from serving.
 */
@QuarkusTest
class HealthProbeCompositionTest {

    @Test
    void readinessProbesTheDatabaseOnceAndS3() {
        given().when().get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"))
                .body("checks.name", hasItem("Database connections health check"))
                .body("checks.name", hasItem("S3 storage")).body("checks.name", not(hasItem("database")));
    }

    @Test
    void livenessIsUpWithoutAnApplicationCheckOfItsOwn() {
        given().when().get("/q/health/live").then().statusCode(200).body("status", equalTo("UP")).body("checks.name",
                not(hasItem("application")));
    }
}
