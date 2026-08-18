package de.ingoschindler.infrastructure.web;

import de.ingoschindler.kernel.problem.ProblemDetails;
import jakarta.inject.Inject;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;

/**
 * Generic fallback for any {@link WebApplicationException} thrown from an inbound REST adapter, including the
 * standard JAX-RS subtypes ({@code NotFoundException}, {@code ForbiddenException}, {@code BadRequestException},
 * ...) and the framework's own "no matching resource" 404. Renders a {@code application/problem+json} body
 * sourced from the exception's response status (with the standard reason phrase as {@code title}) and message
 * (as {@code detail}).
 *
 * <p>This mapper replaces the previous custom {@code WebException} hierarchy: inbound REST adapters now throw
 * {@code jakarta.ws.rs.*} exceptions directly (for the few cases that need a thrown exception) and rely on this
 * mapper to render the RFC 7807 payload. BC-specific exceptions still get their own BC-local mappers — they
 * carry domain-meaningful problem-type URIs and live alongside the exceptions they translate. Trace
 * correlation is handled by OpenTelemetry: {@code traceId} / {@code spanId} land in MDC automatically; trace
 * export is currently disabled pending a collector (see ADR-23), and propagation and MDC correlation remain
 * active.
 */
@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Inject
    RequestUris requestUris;

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse() == null ? 500 : exception.getResponse().getStatus();
        String title = Response.Status.fromStatusCode(status) == null
                ? "Error"
                : Response.Status.fromStatusCode(status).getReasonPhrase();
        String detail = exception.getMessage() == null ? title : exception.getMessage();
        URI instance = requestUris.currentOrNull();
        ProblemDetails body = ProblemDetails.of(status, title, detail, instance);
        return Response.status(status).type(ProblemDetails.MEDIA_TYPE).entity(body).build();
    }
}
