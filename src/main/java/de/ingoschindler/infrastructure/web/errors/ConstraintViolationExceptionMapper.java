package de.ingoschindler.infrastructure.web.errors;

import de.ingoschindler.infrastructure.web.RequestUris;
import de.ingoschindler.kernel.problem.ProblemDetails;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;
import java.util.List;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    RequestUris requestUris;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ProblemDetails.FieldError> errors = exception.getConstraintViolations().stream()
                .map(ConstraintViolationExceptionMapper::toFieldError).toList();
        URI instance = requestUris.currentOrNull();
        ProblemDetails body = ProblemDetails.of(URI.create("urn:starter:error:validation"), 400, "Validation Failed",
                "Validation failed", instance, errors);
        return Response.status(400).type(ProblemDetails.MEDIA_TYPE).entity(body).build();
    }

    private static ProblemDetails.FieldError toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        return new ProblemDetails.FieldError(path, violation.getMessage());
    }
}
