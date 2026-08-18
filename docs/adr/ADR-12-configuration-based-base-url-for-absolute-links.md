# ADR-12: Configuration-Based Base URL for Absolute Links

**Status:** Accepted
**Reversibility:** high — one configuration key.

## Context

Some responses have to carry an absolute URL: a signed download link is useless
as a relative path when the client stores it, mails it, or renders it in a context
without the original origin.

The obvious source is the request. Take the scheme, host and port from the
incoming request and build the URL from that. Behind a reverse proxy it produces
`http://0.0.0.0:8080/...` — the container's own bind address, without TLS, with a
port nobody can reach. The framework's host and port configuration reports the
same thing.

The next obvious source is the `Host` and `X-Forwarded-*` headers. Those are
attacker-controlled input: a request with a forged `Host` produces links pointing
at the attacker's domain, which is a working password-reset phishing primitive in
any application that mails such a link.

## Decision

An explicit configuration key, `starter.api.base-url`, supplied per environment.
Absolute links are built from it and nothing else.

Never from the request, never from forwarded headers, never derived from the HTTP
listener's bind configuration.

The public bootstrap document the frontend fetches before login deliberately does
*not* carry an API base URL. The browser calls the origin it was served from; a
server-side value for link generation is a different thing and telling the client
about it invites it to use the wrong one.

## Rationale

This is one line of configuration against a class of bug that is both hard to
notice (the links look right in development, where the bind address happens to be
reachable) and unpleasant when it lands (host-header injection).

## Consequences

- One more required environment variable per deployment. It appears in
  `.env.example` and in the deployment documentation.
- Development has a working default, so nothing needs configuring to run locally.
- The value is not validated against the actual serving origin. A wrong value
  produces wrong links, which is visible immediately and cheap to fix.

## Related

- ADR-19 — the signed URLs this key is mostly used for
- ADR-24 — why it comes from the environment
