package de.ingoschindler.demo.adapter.in.rest;

import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.infrastructure.web.RequestUris;
import de.ingoschindler.kernel.problem.ProblemDetails;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.net.URI;

/**
 * Turns the domain's "not found" into 404 {@code application/problem+json}.
 *
 * <p>The mapper lives in the adapter, so the use case can throw a domain
 * exception without knowing that HTTP exists. Status codes are a transport
 * concern; the domain only knows that the thing is not there.</p>
 *
 * <p>Logged at {@code DEBUG}, not {@code WARN} (ADR-14): a 404 is a client-side
 * outcome, and logging it louder than that trains everyone to ignore the log
 * during the incident where it matters. The convention across mappers is 4xx at
 * DEBUG, 5xx at ERROR with the dependency, operation and outcome in the message.</p>
 */
@Provider
@Produces(ProblemDetails.MEDIA_TYPE)
public class DemoItemNotFoundExceptionMapper implements ExceptionMapper<DemoItemNotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(DemoItemNotFoundExceptionMapper.class);

    @Inject
    RequestUris requestUris;

    @Override
    public Response toResponse(DemoItemNotFoundException exception) {
        URI instance = requestUris == null ? null : requestUris.currentOrNull();
        LOGGER.debugf("demo_item_not_found status=404 path=%s", instance == null ? "?" : instance.getPath());

        return Response.status(Response.Status.NOT_FOUND).type(ProblemDetails.MEDIA_TYPE)
                .entity(ProblemDetails.of(URI.create("urn:starter:error:not-found"), 404, "Not Found",
                        "The requested demo item does not exist.", instance))
                .build();
    }
}
