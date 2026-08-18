package de.ingoschindler.infrastructure;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Public bootstrap document for the frontend: the OIDC settings it needs before a user is
 * authenticated, and nothing else.
 *
 * <p>Keep it minimal on purpose. An API base URL does not belong here: it is server-side
 * configuration ({@code starter.api.base-url}) for generating absolute links, and the browser
 * calls the same origin it was served from anyway. Deriving one from
 * {@code quarkus.http.host}/{@code port} reports the container bind address {@code 0.0.0.0:8080}
 * without a scheme. Feature flags do not belong here either unless there is a real switch behind
 * them — a hardcoded flag pair is a lie the frontend then branches on.
 *
 * <p>{@code version} is the build's own version, so the answer to "which backend am I talking to"
 * comes from the running artifact rather than a literal someone has to remember to bump.
 */
@Path("/.well-known/app-config")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "App Config")
public class AppConfigResource {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "http://localhost:8180/realms/starter")
    String issuer;

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "frontend-client")
    String clientId;

    /**
     * Recorded at build time from the Gradle project version. Deliberately without a default: a
     * build that cannot state its own version should fail at startup, not serve a plausible lie.
     */
    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @Operation(summary = "Get public application configuration", description = "Returns the OIDC client settings the frontend needs to bootstrap login, plus the version of the running backend build.")
    @APIResponse(responseCode = "200", description = "Application configuration")
    @GET
    public AppConfig config() {
        return new AppConfig(new AuthConfig(clientId, issuer), version);
    }

    public record AppConfig(AuthConfig authConfig, String version) {
    }

    public record AuthConfig(String clientId, String issuer) {
    }
}
