package de.ingoschindler.infrastructure.web.errors;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

/**
 * A rejected payload answers with the field errors, not just a status (ADR-09).
 * One round trip has to be enough for a client to fix its request.
 */
@QuarkusTest
class ConstraintViolationExceptionMapperTest {

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    void missingRequiredFieldReturns400WithFieldErrors() {
        given().contentType("application/json").body("{}").when().post("/api/demo-items").then().statusCode(400)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:starter:error:validation")).body("title", is("Validation Failed"))
                .body("errors", hasSize(1)).body("errors[0].field", containsString("name"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    void oversizedFieldReturns400() {
        String tooLong = "x".repeat(200);

        given().contentType("application/json").body("{\"name\":\"" + tooLong + "\"}").when().post("/api/demo-items")
                .then().statusCode(400).body("errors", hasSize(1));
    }
}
