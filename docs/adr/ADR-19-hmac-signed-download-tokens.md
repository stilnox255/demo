# ADR-19: HMAC-Signed Download Tokens in API Responses

**Status:** Accepted
**Reversibility:** medium — the URL shape is client-visible.

## Context

A browser cannot attach an `Authorization` header to an `<img src>`, to a
`<a download>` link, or to anything else the platform fetches on its own. So a
protected file cannot be referenced from a page the normal way.

The workarounds are all worse than they look:

- **A session cookie.** Now the API has two authentication mechanisms, and the
  cookie one is CSRF-exposed.
- **Fetch the bytes with the bearer token and build a blob URL.** Works, but the
  whole file goes through JavaScript memory, and it breaks a plain download link.
- **Make the endpoint public.** The file is protected for a reason.
- **Guessable-but-obscure URLs.** Not a mechanism, just an absence of one.

## Decision

A short-lived HMAC-SHA256 token in the query string, minted by the server and
embedded in the API response that describes the file:

```
GET /api/demo-items/{id}/attachment?t=<token>
```

The token payload is `resourceId|expiry`, signed with a server-side secret. It is
verified before the resource is read, and:

- It is **bound to one resource id.** A token for one file cannot fetch another.
- It **expires**, by default in five minutes. A leaked URL grants access to one
  file for the remainder of that window.
- Comparison goes through a constant-time digest comparison, not
  `String.equals` — a forged token must not be refinable byte by byte off the
  response time.
- An invalid or expired token answers **404, not 401**: an authentication error
  would confirm that the id exists.

The signing secret has no production default, so a deployment that forgets it
fails at startup rather than minting forgeable tokens.

The URL is assembled by the kernel's token port from a base URL and a path the
caller supplies. The port knows how to sign a URL, not which endpoints exist.

## Rationale

This is the standard pre-signed-URL pattern, implemented in the application rather
than delegated to the object store. Doing it here keeps the authorization decision
in the application — the store has no idea who owns what — and keeps the object
store unreachable from the internet.

## Consequences

- The download endpoint is `@PermitAll` and validates the token itself as its
  first statement. That is a deliberate exception to the class-level role
  requirement, and it is annotated as one.
- A URL in a response is only valid for the token's lifetime. Clients must not
  store it; they re-read the resource to get a fresh one, which the response
  documentation says explicitly.
- The secret is a rotation concern: rotating it invalidates every outstanding
  token, which is acceptable at a five-minute TTL.

## Related

- ADR-12 — where the absolute part of the URL comes from
- ADR-08 — why the failure is a problem-details 404
