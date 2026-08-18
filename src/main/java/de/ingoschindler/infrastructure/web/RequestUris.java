package de.ingoschindler.infrastructure.web;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;

/**
 * Resolves the current request URI for the {@code instance} member of a problem-details body.
 *
 * <p>Exception mappers cannot use {@code UriInfo.getRequestUri()} for this: RESTEasy Reactive fills the
 * request context's absolute URI while matching a resource method, so a request that fails <em>before</em>
 * matching — the framework's own 404 for an unmapped path, an authentication failure raised in an upstream
 * Vert.x handler — reaches the mapper with that field still unset and {@code getRequestUri()} throws a
 * {@link NullPointerException}. The mapper then turns a 404 into a 500 and hides the original error. The
 * underlying Vert.x request always carries the URI, so read it from there instead.
 */
@ApplicationScoped
public class RequestUris {

    @Inject
    CurrentVertxRequest currentVertxRequest;

    /**
     * @return the absolute URI of the request being handled, or {@code null} when there is no current request
     *         or its URI is not parseable — {@code instance} is an optional member of the problem body.
     */
    public URI currentOrNull() {
        var request = currentVertxRequest.getCurrent();
        if (request == null) {
            return null;
        }
        try {
            return URI.create(request.request().absoluteURI());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
