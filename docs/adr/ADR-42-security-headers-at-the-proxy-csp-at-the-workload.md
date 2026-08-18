# ADR-42: Security Headers at the Reverse Proxy, CSP at the Workload

**Status:** Accepted
**Reversibility:** high — header configuration.

## Context

Response security headers have two plausible homes: the shared reverse proxy, or the
service that produces the response. Picking one for everything is wrong in both
directions.

**Everything at the proxy.** Convenient — one definition for every route. But a
Content-Security-Policy is a statement about *who serves the scripts*, and the proxy
fronts both a JSON API and a static frontend. A `script-src` policy applied to the
API's JSON responses is meaningless at best; a policy loose enough to suit both is
loose enough to be worthless.

**Everything at the workload.** Then every service reimplements HSTS, and a new
service ships without it until someone notices.

There is also a header-mechanics trap in the Nginx side that silently disables
whatever it touches: Nginx *replaces* inherited headers rather than merging them as
soon as any `add_header` appears in a nested scope. A `location` block that adds a
cache header therefore drops every security header inherited from the server block.

## Decision

**Transport and framing headers at the proxy**, as a named dynamic middleware
referenced by every router:

- HSTS, one year, `includeSubdomains`, `preload`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-Frame-Options: DENY`
- `Cross-Origin-Opener-Policy` and `Cross-Origin-Resource-Policy`: `same-origin`
- `Permissions-Policy` denying every device capability by listing it with an empty
  allowlist

These are correct for every response regardless of what produces it, and defining
them once means a new router inherits them by name rather than by copy-paste.

**The Content-Security-Policy at the workload** — the Nginx vhost that serves the
frontend. It is where `script-src 'self'` is meaningful, and it is the only place
that knows which origins the application actually talks to.

The policy's allowed auth origin differs per deployment, so the vhost is a
**template rendered at container start** from an environment variable, using the
image's own template mechanism. A hardcoded identity-provider origin in a CSP is the
kind of thing that is discovered when login breaks in a new environment.

Every `location` block that adds any header re-adds the full security set. The
repetition is not redundancy; omitting it unsets them for that location.

## Rationale

The split follows from what each header is a statement about. Transport security is a
property of the connection, so it belongs where the connection terminates. A content
policy is a property of the document, so it belongs with whatever serves the
document.

## Consequences

- The middleware file has to be installed into the reverse proxy host's dynamic
  configuration directory — it is not part of the compose file. Its own header says
  where it goes, because a middleware referenced by name and not present means every
  router fails to load.
- The CSP is duplicated across three `location` blocks in the vhost. Unavoidable
  given Nginx's header semantics, and commented at each occurrence.
- `script-src 'self'` with no `unsafe-inline` is what makes browser-storage tokens
  (ADR-27) an acceptable trade. Loosening it would change that calculation.

## Related

- ADR-25 — the Nginx container that owns the CSP
- ADR-27 — the trade the CSP is mitigating
