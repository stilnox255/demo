package de.ingoschindler.infrastructure.logging;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class UserIdMdcFilterTest {

    @Path("/test/mdc/whoami")
    public static class WhoAmIResource {

        @Inject
        SecurityIdentity identity;

        @GET
        public Response get() {
            return Response.ok(MDC.get("userId") == null ? "none" : MDC.get("userId").toString()).build();
        }
    }

    @Test
    @TestSecurity(user = "alice", roles = "user")
    void authenticatedRequestGetsUserIdInMdc() {
        given().when().get("/test/mdc/whoami").then().statusCode(200).body(is("alice"));
    }

    @Test
    void anonymousRequestGetsNoUserIdKey() {
        given().when().get("/test/mdc/whoami").then().statusCode(200).body(is("none"));
    }

    @Test
    void invalidBearerTokenOnPermitAllEndpointGetsNoUserIdKeyNot401() {
        // Regression test for the bug found while verifying this filter: with
        // quarkus.http.auth.proactive=false, merely resolving SecurityIdentity is what triggers
        // credential validation. A malformed/unknown bearer token on this permit-all resource must
        // not turn into a hard 401 — it must be treated the same as an anonymous request.
        given().header("Authorization", "Bearer bogus-garbage-token").when().get("/test/mdc/whoami").then()
                .statusCode(200).body(is("none"));
    }
}
