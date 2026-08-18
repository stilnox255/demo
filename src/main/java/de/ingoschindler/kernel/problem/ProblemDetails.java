package de.ingoschindler.kernel.problem;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;

import java.net.URI;
import java.util.List;

/**
 * RFC 7807 / RFC 9457 Problem Details body. Validation failures additionally populate {@link FieldError}
 * entries; for non-validation errors the {@code errors} array is omitted (Jsonb null-handling: see
 * {@link JsonbNillable @JsonbNillable(false)}).
 *
 * <p>Lives in the shared kernel so every BC's REST mapper and any infrastructure provider can produce the
 * same wire shape without each touching a JAX-RS-coupled class. The record carries pure JSON-B annotations
 * only — no JAX-RS, no Quarkus, no BC-specific types.
 *
 * <p>Trace correlation is handled by OpenTelemetry: callers propagate the W3C {@code traceparent} header,
 * and Quarkus' OTel integration writes {@code traceId} / {@code spanId} into MDC so log lines and traces
 * stitch together without an extension field on the response body.
 */
public record ProblemDetails(URI type, String title, int status, String detail, URI instance,
        @JsonbNillable(false) @JsonbProperty("errors") List<FieldError> errors) {

    public static final URI DEFAULT_TYPE = URI.create("about:blank");

    public static final String MEDIA_TYPE = "application/problem+json";

    public ProblemDetails {
        if (type == null) {
            type = DEFAULT_TYPE;
        }
        if (errors != null && errors.isEmpty()) {
            errors = null;
        }
    }

    public static ProblemDetails of(int status, String title, String detail, URI instance) {
        return new ProblemDetails(DEFAULT_TYPE, title, status, detail, instance, null);
    }

    public static ProblemDetails of(int status, String title, String detail, URI instance, List<FieldError> errors) {
        return new ProblemDetails(DEFAULT_TYPE, title, status, detail, instance, errors);
    }

    /**
     * Overload for problem responses that want a custom {@code type} URI (e.g.
     * {@code urn:problem:ingest:format-mismatch}) instead of the default {@code about:blank}. BC-local mappers
     * use this to publish a stable, machine-readable error code in the body without changing the rest of the
     * shape.
     */
    public static ProblemDetails of(URI type, int status, String title, String detail, URI instance) {
        return new ProblemDetails(type, title, status, detail, instance, null);
    }

    /**
     * Overload for problem responses that combine a custom {@code type} URI with a validation-error list (e.g.
     * {@code urn:starter:error:validation}).
     */
    public static ProblemDetails of(URI type, int status, String title, String detail, URI instance,
            List<FieldError> errors) {
        return new ProblemDetails(type, title, status, detail, instance, errors);
    }

    public record FieldError(String field, String message) {
    }
}
