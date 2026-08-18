package de.ingoschindler.infrastructure.web.errors;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * REST-level coverage of the generic exception mappers under {@code infrastructure.web} /
 * {@code infrastructure.web.errors}. After v2 the platform no longer carries a custom {@code WebException}
 * hierarchy: inbound REST adapters throw {@code jakarta.ws.rs.*} exceptions directly for the rare cases that
 * need it, and the generic {@code WebApplicationExceptionMapper} renders the RFC 7807 problem+json. This test
 * locks in that contract end-to-end. Trace correlation is left to OpenTelemetry — the response body no longer
 * carries a {@code correlationId} extension field.
 */
@QuarkusTest
class ExceptionMappersTest {

    @Test
    void jakarta_not_found_exception_renders_404_problem_json() {
        given().when().get("/test/errors/not-found").then().statusCode(404).contentType("application/problem+json")
                .body("type", equalTo("about:blank")).body("title", equalTo("Not Found")).body("status", is(404))
                .body("detail", equalTo("import 123 not found"))
                .body("instance", containsString("/test/errors/not-found")).body("correlationId", nullValue());
    }

    @Test
    void jakarta_forbidden_exception_maps_to_403() {
        given().when().get("/test/errors/forbidden").then().statusCode(403).contentType("application/problem+json")
                .body("status", is(403)).body("title", equalTo("Forbidden"));
    }

    @Test
    void jakarta_not_supported_exception_maps_to_415() {
        given().when().get("/test/errors/unsupported-format").then().statusCode(415)
                .contentType("application/problem+json").body("status", is(415))
                .body("title", equalTo("Unsupported Media Type"));
    }

    @Test
    void client_error_exception_with_conflict_status_maps_to_409() {
        given().when().get("/test/errors/conflict").then().statusCode(409).contentType("application/problem+json")
                .body("status", is(409));
    }

    @Test
    void validation_failure_returns_errors_list() {
        given().when().get("/test/errors/validate").then().statusCode(400).contentType("application/problem+json")
                .body("status", is(400)).body("title", equalTo("Validation Failed")).body("errors.size()", is(1))
                .body("errors[0].field", containsString("name"))
                .body("errors[0].message", equalTo("name must not be blank"));
    }

    @Test
    void unhandled_throwable_returns_500_with_generic_detail() {
        given().when().get("/test/errors/boom").then().statusCode(500).contentType("application/problem+json")
                .body("status", is(500)).body("detail", equalTo("An unexpected error occurred"))
                .body("detail", not(containsString("secret stacktrace marker")));
    }

    @Test
    void runtime_404_from_unknown_path_renders_problem_json() {
        given().when().get("/test/errors/this-path-does-not-exist").then().statusCode(404)
                .contentType("application/problem+json").body("status", is(404));
    }

    @ApplicationScoped
    @Path("/test/errors")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TestErrorResource {

        @GET
        @Path("/not-found")
        public String notFound() {
            throw new NotFoundException("import 123 not found");
        }

        @GET
        @Path("/forbidden")
        public String forbidden() {
            throw new ForbiddenException("orgId mismatch");
        }

        @GET
        @Path("/unsupported-format")
        public String unsupported() {
            throw new NotSupportedException("DATEV is not supported yet");
        }

        @GET
        @Path("/conflict")
        public String conflict() {
            throw new ClientErrorException("duplicate upload", Response.Status.CONFLICT);
        }

        @GET
        @Path("/boom")
        public String boom() {
            throw new IllegalStateException("secret stacktrace marker - must not leak");
        }

        @GET
        @Path("/validate")
        public String validate(@QueryParam("name") @NotBlank(message = "name must not be blank") String name) {
            return name;
        }
    }
}
