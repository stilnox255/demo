package de.ingoschindler.infrastructure;

import de.ingoschindler.demo.application.port.in.DemoItemQueryPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.json.bind.JsonbException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The catch-all mapper: everything that reaches it must still answer
 * {@code application/problem+json} (ADR-08), and a 500 must not leak internals.
 */
@QuarkusTest
class GlobalExceptionMapperTest {

    @InjectMock
    DemoItemQueryPort query;

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    void jsonbExceptionReturns400WithProblemDetail() {
        when(query.byIdForOwner(any(), any())).thenThrow(new JsonbException("Invalid UUID string: C0CEAc6c"));

        given().pathParam("id", UUID.randomUUID()).when().get("/api/demo-items/{id}").then().statusCode(400)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:starter:error:bad-request")).body("title", is("Bad Request"))
                .body("status", is(400)).body("detail", is("Invalid UUID string: C0CEAc6c"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    void illegalArgumentReturns400WithProblemDetail() {
        when(query.byIdForOwner(any(), any())).thenThrow(new IllegalArgumentException("Invalid UUID string: eEEA8"));

        given().pathParam("id", UUID.randomUUID()).when().get("/api/demo-items/{id}").then().statusCode(400)
                .contentType(containsString("application/problem+json"))
                .body("type", is("urn:starter:error:bad-request")).body("status", is(400))
                .body("detail", is("Invalid UUID string: eEEA8"));
    }

    /**
     * The message of the original exception carries a password here on purpose:
     * whatever the cause, the response body may only ever say that something went
     * wrong. Detail belongs in the log, where the caller cannot read it.
     */
    @Test
    @TestSecurity(user = "test-user", roles = "user")
    void uncaughtExceptionReturns500WithoutLeakingTheCause() {
        when(query.byIdForOwner(any(), any())).thenThrow(new RuntimeException("DB error: password=hunter2"));

        given().pathParam("id", UUID.randomUUID()).when().get("/api/demo-items/{id}").then().statusCode(500)
                .contentType(containsString("application/problem+json"))
                .body("detail", is("An unexpected error occurred")).body("detail", not(containsString("hunter2")));
    }
}
