# Backend — Quarkus-specific Rules

Rules for backend code, on top of the layering in
`docs/adr/ADR-01-hexagonal-architecture.md`.

## Naming

- Resources: plural `*Resource` (`DemoItemsResource`)
- Use cases: `{Verb}{Noun}UseCase` (in-port) implemented by `{Verb}{Noun}Service`
- Read facades: `{Noun}QueryPort` implemented by `{Noun}Query` — no `Service` suffix
- Aggregates: singular nouns (`DemoItem`)
- JPA entities: `{Aggregate}JpaEntity`, only in `adapter.out.persistence`
- DTOs: separate records in `adapter.in.rest.dto`, never nested in a domain type

## Observability

Metrics via Micrometer/Prometheus, distributed tracing via OpenTelemetry —
independent concerns, not alternatives. Correlation ids go through the MDC and appear
in every log line (ADR-23).

## Transaction Boundaries (ADR-04)

Split a use case rather than let its transaction grow. If a flow needs a remote call
— object storage, an HTTP API — between reading and writing an aggregate, that is
**not one use case**:

- Each database step is its own use case class with its own in-port and
  `@Transactional(REQUIRES_NEW)` on the service method. `REQUIRES_NEW`, not the
  default — an orchestrator may itself be called from a transactional context, and
  `REQUIRED` would silently enrol every step in that one.
- The class sequencing them is an orchestrator and is **never** `@Transactional`.
  Remote calls happen there, with no transaction open.
- A step re-reads the aggregate it writes. Never pass in an instance loaded before a
  remote call — the write would discard concurrent changes.
- Batch loops use one transaction per item, so one failure or optimistic-lock
  conflict cannot discard the whole batch.

A step that needs its own boundary lives in **its own bean**, not in a private
method: `@Transactional` is a CDI interceptor and a self-call bypasses the proxy, so
the annotation would silently do nothing. See `DemoItemAttachmentRecorder` for the
pattern.

Do not use `QuarkusTransaction` in the application layer, do not put transaction
boundaries on repository ports, and do not give ports callback-shaped signatures
(`mutate(id, Consumer)`) to smuggle a boundary in. `@Transactional` on a use case is
the only mechanism.

## Repository Ports

- Every read is **owner-scoped at the port level**. There is deliberately no
  `findById(UUID)` to reach for: an ownership check in the caller is one somebody
  forgets, and that is how an IDOR ships.
- An exception needs a name that looks like one: `findByIdForSignedAccess(UUID)` for
  the token-authorized download path, so using it looks like the exception it is.
- Filters belong in the query, never as a `.filter()` after a `list()`.
- `PanacheQuery` never leaves `adapter.out.persistence`. Pagination converts to the
  kernel `Page` through `PanachePages`.

## Panache Query Pattern

```java
DemoItemJpaEntity.<DemoItemJpaEntity>find("ownerId = :ownerId",
        NEWEST_FIRST, Map.of("ownerId", ownerId))
```

`Map`, not `Parameters` — the latter is deprecated. `Sort` constants are static
fields, so the ordering is stated once.

## JAX-RS

- `Response.created(uri).entity(dto)` for POST, with the URI from `UriInfo` — never a
  hand-assembled string (ADR-12)
- `Response.accepted()` for anything asynchronous
- `ownerId()` from `SecurityIdentity`, never from the request body or a path
  parameter
- **List endpoints always return a paginated envelope** using `PagedResult<T>` with
  `page` (default 1) and `pageSize` (default 25, max 100). Build it with
  `PagedResult.of(Page<T>, mapper)`, never by assembling `PageMeta` in the resource.
  An empty result reports `totalPages: 0` (ADR-13).
- Every endpoint carries `@Operation` and an `@APIResponse` per status a client must
  handle, including the error ones (ADR-11)

## Testing

- Unit tests for domain and use cases: plain JUnit, a hand-written fake repository,
  a fixed `Clock`. No `@QuarkusTest`. If a use-case test needs the container, the
  layering is wrong.
- `@QuarkusTest` for the HTTP contract, with `@TestSecurity(user = "…", roles = "…")`
  and RestAssured. `@InjectMock` for outbound ports.
- Mocking a framework interface at a boundary is fine. Static mocking, spies,
  reflection into internals and resetting global state are not — each says a seam is
  missing.
- A guard or a fitness function needs a demonstration that it fails when it should.
  A safeguard that cannot fire is worse than none, because it also stops anyone from
  looking.
