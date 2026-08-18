package de.ingoschindler.infrastructure;

import de.ingoschindler.infrastructure.web.RequestUris;
import de.ingoschindler.kernel.problem.ProblemDetails;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;
import org.jboss.logging.Logger;

/**
 * Authoritative fallback for any {@link Throwable} not already handled by a more specific mapper. Note that
 * {@link jakarta.ws.rs.WebApplicationException} (and its subtypes, e.g. {@code NotFoundException}) are always
 * intercepted first by {@link de.ingoschindler.infrastructure.web.WebApplicationExceptionMapper}, which is
 * registered for the narrower type — JAX-RS resolves exception mappers by nearest matching type in the
 * exception's class hierarchy, so this mapper never sees a {@code WebApplicationException}.
 */
@Provider
@Produces("application/problem+json")
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    RequestUris requestUris;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @Context
    ContainerRequestContext containerRequestContext;

    @Override
    public Response toResponse(Throwable exception) {
        return switch (exception) {
            case JsonbException jsonb ->
                problemResponse(400, "urn:starter:error:bad-request", "Bad Request", jsonb.getMessage());

            case IllegalArgumentException badArg ->
                problemResponse(400, "urn:starter:error:bad-request", "Bad Request", badArg.getMessage());

            default -> {
                URI instance = requestUris == null ? null : requestUris.currentOrNull();
                String method = containerRequestContext == null ? "?" : containerRequestContext.getMethod();
                String path = instance == null ? "?" : instance.getPath();

                if (responseAlreadyCommitted()) {
                    // Vert.x itself already flushed a response (e.g. its body-limit handler
                    // rejecting an oversized request with 413) before RESTEasy finished reading the
                    // request and surfaced this exception. The client already has a correct response;
                    // writing another one here would just throw IllegalStateException deep in RESTEasy's
                    // writer pipeline. No entity: that's what stops RESTEasy from opening a second
                    // output stream on the connection Vert.x already closed.
                    LOGGER.debugf(exception,
                            "unhandled_exception dependency=self operation=%s %s status=500 exception=%s "
                                    + "response_already_committed=true",
                            method, path, exception.getClass().getName());
                    yield Response.status(500).build();
                }

                String message = "unhandled_exception dependency=self operation=%s %s status=500 exception=%s"
                        .formatted(method, path, exception.getClass().getName());
                LOGGER.error(message, exception);
                yield problemResponse(500, "about:blank", "Internal Server Error", "An unexpected error occurred");
            }
        };
    }

    private boolean responseAlreadyCommitted() {
        var routingContext = currentVertxRequest == null ? null : currentVertxRequest.getCurrent();
        return routingContext != null && routingContext.response().ended();
    }

    private Response problemResponse(int status, String type, String title, String detail) {
        return Response.status(status).type("application/problem+json")
                .entity(ProblemDetails.of(URI.create(type), status, title, detail, null)).build();
    }
}
