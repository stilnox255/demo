# ADR-08: RFC 9457 Problem Details for Errors

**Status:** Accepted
**Reversibility:** medium — the error body is part of the contract, but only the error path.

## Context

Error responses get invented per endpoint. One returns `{"error": "not found"}`,
another a bare string, a third an HTML page from the container's default error
handler. A client then needs a parser per endpoint, and in practice writes none:
it checks the status code and shows a generic message, which is how "something
went wrong" becomes the entire error experience.

The framework's default is part of the problem. An unmapped exception produces
whatever the container decides, usually an HTML page, from a JSON API.

## Decision

RFC 9457 Problem Details for every error response, with
`Content-Type: application/problem+json`:

```json
{
  "type": "urn:starter:error:validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/demo-items",
  "errors": [ { "field": "name", "message": "must not be blank" } ]
}
```

- `type` is a URN a client can branch on, stable across wording changes.
  `about:blank` where the status alone says everything.
- `title` is human-readable and short; `detail` is specific to this occurrence.
- `instance` is the request path, filled from the request URI where one is
  available.
- `errors` is a documented extension member for field-level validation, so a
  client can annotate a form from one response.

Every layer that can produce an error produces this shape: bean-validation
failures, persistence failures, domain exceptions, and a catch-all mapper for
anything unmapped. The catch-all matters most — it is what stops the container's
HTML page from reaching a client.

**A 500 never leaks the cause.** `detail` is a fixed generic sentence; the
exception goes to the log with the correlation ids. An exception message is a
gift to an attacker and rarely useful to the caller.

## Rationale

The standard exists, is implemented by client libraries, and answers the
questions a house format would have to answer anyway — a machine-readable code, a
human message, a per-occurrence detail. Inventing one costs the same effort and
the documentation on top.

## Consequences

- Error mappers are infrastructure, not per-resource code.
- The `errors` extension is non-standard but documented; RFC 9457 explicitly
  allows extension members.
- Log level per mapper follows ADR-14, so a 404 does not arrive as an error.

## Related

- ADR-09 — what produces the validation errors
- ADR-14 — how these responses are logged
