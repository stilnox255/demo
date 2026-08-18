package de.ingoschindler.infrastructure;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * The bootstrap document is fetched before login, so it must answer unauthenticated. Comparing
 * {@code version} against the injected build version rather than a literal is what makes this test
 * useful: it fails if the field is hardcoded again, if the config key is wrong, or if the value is
 * empty, and it survives a version bump.
 */
@QuarkusTest
class AppConfigResourceTest {

    @ConfigProperty(name = "quarkus.application.version")
    String buildVersion;

    @Test
    void servesOidcSettingsAndTheRunningBuildVersionWithoutAuthentication() {
        given().when().get("/.well-known/app-config").then().statusCode(200).body("version", is(buildVersion))
                .body("version", not(emptyOrNullString())).body("authConfig.clientId", not(emptyOrNullString()))
                .body("authConfig.issuer", not(emptyOrNullString()));
    }
}
