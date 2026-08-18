# ADR-11: OpenAPI Annotations on All Endpoints

**Status:** Accepted
**Reversibility:** high — annotations only.

## Context

A generated OpenAPI document is free: the framework derives paths, methods and
types from the JAX-RS annotations without any help. What it cannot derive is
*meaning*. The result is a type listing — every endpoint present, none of them
explained, every response documented as "default response".

A type listing is worse than no document, because it looks like documentation. A
client author reads it, learns nothing, and stops trusting it.

## Decision

Every endpoint carries:

- `@Operation` with a summary, and a description wherever the behaviour is not
  obvious from the path. The description is where the non-obvious contract goes:
  that a status change requires `expectedVersion`, that a download is authorized
  by a signed token rather than the bearer token, that a summary endpoint answers
  304.
- `@APIResponse` for each status a client must handle. The error ones matter most
  — 400, 404, 409 — because they are the responses a client has to write code
  for, and the ones an implementation-derived document never mentions.
- `@Parameter` on query and path parameters whose name does not fully explain
  them.
- `@Schema` on the DTO records and their fields (ADR-10), with an example where a
  format is not self-evident.
- `@Tag` on the resource class, so related endpoints group in the rendered
  document.

The document is served at a fixed path with an interactive UI in development.

## Rationale

The annotations sit next to the code they describe, which is the only place
documentation survives. A separate specification file drifts within a sprint.

The specific insistence on documenting error responses comes from what clients
actually get wrong: a 409 nobody documented is a 409 nobody handles, and the
first time anyone finds out is in production.

## Consequences

- A new endpoint is not complete until it is annotated. That is a review item.
- The document is generated from the running application's own types, so it cannot
  describe a response the code does not produce.

## Related

- ADR-08 — the error bodies these responses reference
- ADR-10 — the typed records that make the schema meaningful
