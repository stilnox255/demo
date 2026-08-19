# ADR-45: Tokens Are Validated Locally, Never Introspected

**Status:** Accepted
**Reversibility:** high — three configuration properties. Nothing client-visible changes.

## Context

Every request arrives with a bearer token, and the backend has to answer two
questions about it before doing anything else: is it genuine, and does it still mean
what it said when it was issued. There are two mechanisms, and the choice between
them is usually presented as performance versus security, which is the wrong axis.

- **Local validation.** Verify the signature against the provider's public key,
  check the claims, done. No network call.
- **Introspection.** Ask the provider on every request whether the token is still
  good.

The real axis is where an outage lands and how stale an answer you are willing to
serve. Introspection puts the identity provider in the hot path of every single
request; local validation accepts that a token stays valid for its remaining
lifetime even after the user behind it was disabled.

## Decision

Local validation, with introspection switched off explicitly rather than left at a
default:

```properties
quarkus.oidc.application-type=service
quarkus.oidc.token.allow-jwt-introspection=false
quarkus.oidc.token.allow-opaque-token-introspection=false
%prod.quarkus.oidc.roles.source=accesstoken
%prod.quarkus.oidc.token.forced-jwk-refresh-interval=PT10M
```

**The backend holds no key material.** This is the part that is worth stating
plainly, because the instinct says otherwise. Keycloak signs with an RSA private key
that never leaves it (`defaultSignatureAlgorithm: RS256` in the realm). Verifying
needs the matching *public* key, which the provider publishes unauthenticated at its
JWKS endpoint. The backend fetches that once, caches it, and from then on validates
in-process.

A client secret is a different credential for a different job: it authenticates the
application *to* Keycloak, for the token endpoint, introspection, or a client
credentials grant. A resource server that only reads tokens calls none of those and
therefore needs none of it. Symmetric signing (HS256) would collapse the two, which
is exactly why it is not used here: anything that can verify a token can also mint
one, and a compromised resource server becomes a token factory for the whole realm.

**What is actually checked** is more than the signature: the issuer against the
configured `auth-server-url`, `exp` and `nbf`, and the audience, which is why the
realm carries an audience mapper rather than relying on the default claim set.
Roles come from the access token, so the realm has to put them there.

**Key rotation is handled by the `kid`.** Every token names the key that signed it.
An unfamiliar `kid` triggers one JWKS refetch, rate-limited to once per ten minutes
so that a flood of garbage tokens cannot turn into a flood of requests to Keycloak.
Rotating a signing key therefore needs no deployment.

## Rationale

Introspection makes the identity provider a hard dependency of every request. A
Keycloak that is slow makes the whole API slow; a Keycloak that is down makes the
whole API down, including the parts that would happily have kept serving. That is
the failure mode ADR-21 exists to avoid, arriving through a different door.

Local validation fails independently. An instance with a warm JWKS cache keeps
validating tokens through a provider outage. What stops is new logins, which is the
correct thing to stop, because that is the part that genuinely requires the provider.

The cost is bounded and it is a knob, not a mystery: revocation takes effect after at
most one access-token lifetime.

## Alternatives considered

**Introspection on every request.** Zero revocation lag, which is the only thing it
buys. In exchange: a round trip per request, the provider in the hot path, and client
credentials to manage and rotate. For an application whose access tokens live five
minutes, that is a large permanent cost against a small transient one.

**Introspection with the results cached.** Rebuilds the token lifetime out of second
parts, and lands on the same staleness window it was introduced to remove — only now
with a cache to invalidate and a new thing to explain.

**HS256 with a shared secret.** Fewer moving parts, and it hands every verifier the
ability to forge. Not a trade-off, a mistake.

**Pushing revocations to the backend.** A revocation list or an event stream from
Keycloak. Distributed state with its own failure modes, built to close a five-minute
window that the token lifetime already closes.

**A longer access-token lifetime.** Cheaper in refreshes, and it lengthens the
revocation window linearly. Five minutes is the point where neither cost dominates.

## Consequences

- **Revocation is delayed by up to `accessTokenLifespan`**, 300 seconds in
  `deploy/keycloak-realm.json`. Anyone raising that value is lengthening the window
  in which a disabled user still gets served, and should know it. The refresh path is
  the exception: it runs against Keycloak, so a disabled user's next refresh fails
  immediately and the frontend turns that into a session-expired toast (ADR-27).
- **A cold start needs Keycloak reachable.** OIDC discovery happens at startup.
  `quarkus.oidc.connection-delay` turns that into a bounded retry; the starter does
  not set it, so a restart during a provider outage does not come back up. Running
  instances are unaffected.
- **The realm is part of this decision, not just configuration.** It has to emit
  realm roles into the access token and an audience the backend can check. A realm
  built by hand, without the audience mapper, produces tokens that authenticate and
  then fail authorization for reasons that are hard to see from the backend logs.
- **The deployment holds no client credentials, and that is load-bearing.** There is
  no Keycloak secret to generate, mount, rotate or copy out of the admin console, and
  the realm needs only one public client. An earlier version of this stack did carry a
  confidential `backend` client whose secret travelled through compose, the entrypoint
  and `setup-secrets.sh` into `quarkus.oidc.credentials.secret` — where, with both
  introspection paths off, nothing ever read it. Dev mode had already been running
  without one for as long as the profile existed, which is what gave it away. Anyone
  adding a confidential client back should be able to name the call that needs it.

## Related

- ADR-27 — the frontend half: refresh is where a revocation is actually felt
- ADR-21 — the same reasoning applied to readiness, and why Keycloak is not in it
- ADR-19 — the other token in this system, HMAC rather than JWT, and why
- ADR-24 — the issuer URL and realm come from the environment, not the artifact
