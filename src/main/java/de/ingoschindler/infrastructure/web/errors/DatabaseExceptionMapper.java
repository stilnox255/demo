package de.ingoschindler.infrastructure.web.errors;

import de.ingoschindler.kernel.problem.ProblemDetails;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.StaleStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import java.net.URI;

/**
 * Maps persistence failures to problem details.
 *
 * <p>Log levels follow the convention in ADR-14: an outcome the client caused and
 * can act on is logged at INFO without a stacktrace, and only a genuine
 * server-side failure gets ERROR with the throwable. Logging a 409 at ERROR is how
 * an error log fills with normal traffic, and after that nobody reads it during
 * the incident where it matters.</p>
 */
@Provider
@Produces("application/problem+json")
public class DatabaseExceptionMapper implements ExceptionMapper<PersistenceException> {

    private static final Logger LOGGER = Logger.getLogger(DatabaseExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException exception) {
        // Optimistic lock conflict (@Version): someone else changed the row
        // between our read and our write. 409, not 500 — the caller's request was
        // well-formed and re-reading and retrying is the correct response.
        if (exception instanceof OptimisticLockException || exception.getCause() instanceof StaleStateException) {
            LOGGER.infof("concurrent_modification status=409 exception=%s", exception.getClass().getName());
            return Response.status(Response.Status.CONFLICT).type("application/problem+json")
                    .entity(ProblemDetails.of(URI.create("urn:starter:error:concurrent-modification"), 409,
                            "Concurrent Modification",
                            "The record was modified by another request. Re-read it and retry.", null))
                    .build();
        }

        if (exception.getCause() instanceof ConstraintViolationException) {
            LOGGER.infof("constraint_violation status=409 exception=%s", exception.getCause().getClass().getName());
            return Response.status(Response.Status.CONFLICT).type("application/problem+json")
                    .entity(ProblemDetails.of(URI.create("urn:starter:error:constraint-violation"), 409,
                            "Constraint Violation", "The record already exists or violates a constraint.", null))
                    .build();
        }
        LOGGER.errorf(exception, "database_error dependency=database status=500 exception=%s",
                exception.getClass().getName());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type("application/problem+json")
                .entity(ProblemDetails.of(500, "Database Error", "An unexpected database error occurred", null))
                .build();
    }
}
