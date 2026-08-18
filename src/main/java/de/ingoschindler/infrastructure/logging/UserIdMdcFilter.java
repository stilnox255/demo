package de.ingoschindler.infrastructure.logging;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationException;
import io.quarkus.security.identity.SecurityIdentity;
import org.jboss.logging.MDC;

/**
 * Puts the authenticated user's principal name into MDC as {@code userId} so JSON log lines
 * for the request are attributable to a user, not just a request shape. Runs after OIDC
 * authentication (unlike the {@code @PreMatching} {@link LoggingFilter}) so {@link SecurityIdentity}
 * is resolved; anonymous requests get no {@code userId} key at all — see ADR-23.
 *
 * <p>{@code quarkus.http.auth.proactive=false} in this app means resolving {@link SecurityIdentity}
 * is what triggers credential validation in the first place. Since most paths are {@code permit-all}
 * and may carry no credentials, or stale/invalid ones, resolving identity here must not turn an
 * otherwise-public request into a hard 401: any {@link AuthenticationException} is treated the same
 * as an anonymous request, and authorization for endpoints that actually require it is still enforced
 * downstream by their own {@code @RolesAllowed}/{@code @Authenticated} checks.
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class UserIdMdcFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String MDC_USER_ID = "userId";

    @Inject
    SecurityIdentity identity;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            if (!identity.isAnonymous()) {
                MDC.put(MDC_USER_ID, identity.getPrincipal().getName());
            }
        } catch (RuntimeException e) {
            // AuthenticationException is a marker interface, not a Throwable, so it can't be caught
            // directly; io.quarkus.security.AuthenticationFailedException/AuthenticationCompletionException
            // both implement it but extend different RuntimeException subtypes.
            if (!(e instanceof AuthenticationException)) {
                throw e;
            }
            // Invalid/stale credentials on an otherwise-public request: treat as anonymous here and
            // let any endpoint that actually requires authentication reject it downstream.
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(MDC_USER_ID);
    }
}
