package de.ingoschindler.infrastructure.logging;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * Logs every inbound request at entry and on response so the platform-level boundary calls always show up in the
 * JSON log stream (see docs/guidelines/release-it.md). Pulls method, path, status, duration_ms from the JAX-RS
 * contexts; {@code traceId}/{@code spanId} are written to MDC by the OpenTelemetry integration for every
 * request, and {@code userId} is written by {@link UserIdMdcFilter} for authenticated requests. No request
 * bodies or auth headers are logged.
 */
@Provider
@PreMatching
@Priority(Priorities.USER + 100)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String MDC_METHOD = "http.method";
    static final String MDC_PATH = "http.path";
    static final String MDC_STATUS = "http.status";
    static final String MDC_DURATION = "http.duration_ms";

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    private static final String START_TIME_PROPERTY = "de.ingoschindler.request.start";

    @Override
    public void filter(ContainerRequestContext request) {
        long startNanos = System.nanoTime();
        request.setProperty(START_TIME_PROPERTY, startNanos);
        String method = request.getMethod();
        String path = request.getUriInfo().getPath();
        MDC.put(MDC_METHOD, method);
        MDC.put(MDC_PATH, path);
        LOG.infof("request_received method=%s path=%s", method, path);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        Object startProperty = request.getProperty(START_TIME_PROPERTY);
        long durationMs = 0L;
        if (startProperty instanceof Long startNanos) {
            durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        }
        int status = response.getStatus();
        MDC.put(MDC_STATUS, status);
        MDC.put(MDC_DURATION, durationMs);
        String method = request.getMethod();
        String path = request.getUriInfo().getPath();
        if (status >= 500) {
            LOG.errorf("request_completed method=%s path=%s status=%d duration_ms=%d", method, path, status,
                    durationMs);
        } else if (status >= 400) {
            LOG.warnf("request_completed method=%s path=%s status=%d duration_ms=%d", method, path, status, durationMs);
        } else {
            LOG.infof("request_completed method=%s path=%s status=%d duration_ms=%d", method, path, status, durationMs);
        }
        MDC.remove(MDC_METHOD);
        MDC.remove(MDC_PATH);
        MDC.remove(MDC_STATUS);
        MDC.remove(MDC_DURATION);
    }
}
