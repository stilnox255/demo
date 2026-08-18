# ADR-05: RESTful Resource Paths

**Status:** Accepted
**Reversibility:** low once clients exist — a path is a published contract.

## Context

Endpoint paths accumulate by accident. Each one is named by whoever added it, in
the vocabulary of the moment, and the result is an API where the caller has to
learn each endpoint separately: a verb in one path, a noun in the next, a
singular here and a plural there.

The alternative is not aesthetic. A predictable path means a client can guess the
next endpoint correctly, and a reviewer can tell whether a new one is
well-formed without asking.

## Decision

Resources are plural nouns. Identity is a path segment. Sub-resources nest under
their parent. Actions are HTTP methods, not path segments.

```
GET    /api/demo-items                 list, paginated
POST   /api/demo-items                 create
GET    /api/demo-items/{id}            read one
PUT    /api/demo-items/{id}            replace
DELETE /api/demo-items/{id}            delete
POST   /api/demo-items/{id}/attachment attach a file to one item
GET    /api/demo-items/{id}/attachment read that file
```

Filtering, sorting and pagination are query parameters, never path segments:
`/api/demo-items?status=ACTIVE`, not `/api/demo-items/active`. A path segment
creates a second endpoint to document, secure and cache; a query parameter is one
endpoint with an argument.

Two deliberate exceptions:

- **An operation that is genuinely not a resource state change** gets a verb
  sub-path with `POST`: `POST /api/demo-items/archive-stale`. Modelling a batch
  job as a resource would be a fiction, and the fiction is more confusing than the
  verb.
- **Collection-wide projections** get a named sub-path:
  `GET /api/demo-items/summary`. It is a different representation of the same
  collection, and it is a different cache and permission story (ADR-41).

`POST` returns 201 with a `Location` header built from the container's own URI
info, never from a hand-assembled string — behind a reverse proxy the container
only knows its bind address.

## Rationale

This is the boring, conventional answer, which is the point. Every one of these
rules is what a reader already expects, so the API needs no explaining and a
deviation is visible.

## Consequences

- A reviewer can judge a new endpoint against the list above.
- Verb-shaped paths need justifying in the pull request. There are two in the
  reference implementation, both annotated with why.

## Related

- ADR-06 — the names of the query parameters
- ADR-07 — why there is no version prefix
- ADR-11 — how each endpoint documents itself
