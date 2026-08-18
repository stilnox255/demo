# ADR-41: Snapshot Cache with the ETag in the Cache Entry

**Status:** Accepted
**Reversibility:** medium — the cache name is versioned, so a change is a bump; the endpoint contract is client-visible.

## Context

One endpoint in an application is usually polled far more often than the rest: a
client asks "did anything change" on every app start, on every return to the
foreground, or on a timer. Each of those requests hits the database for a result
that is identical to the last one, and the cost scales with the number of open
clients rather than with the amount of work being done.

Two savings are available and they are independent:

- **Not querying the database** — a cache.
- **Not sending the body** — a conditional GET, where the client presents the
  validator it received last time and the server answers 304 with nothing.

Doing only the first still sends the full payload to every poller. Doing only the
second still queries the database to find out whether anything changed.

## Decision

A per-owner cached snapshot whose value carries **both** the payload and its HTTP
validator, plus a conditional GET on the endpoint in front of it.

```java
@CacheResult(cacheName = CACHE_NAME)
public Snapshot snapshot(@CacheKey String ownerId) { ... }

public record Snapshot(String etag, List<DemoItemSummary> items) { }
```

Redis in deployed environments, the in-process backend in tests — one annotation
switched by configuration, not a second code path.

Seven decisions inside this one, each of which has a wrong-but-obvious alternative:

**1. The ETag lives in the same cache entry as the payload.** One entry means one
invalidation. An ETag cached separately from its body is a race with a wrong answer
at the end of it. The value is a content hash plus an item count, so it moves
exactly when the payload moves.

**2. The cache bean has exactly one cached method and no internal callers.**
`@CacheResult` is an interceptor, so a self-call does not pass through the proxy. A
convenience wrapper inside the same class would silently bypass the cache while
still returning correct answers — a performance bug with no symptom. Anything that
needs a fallback or a filter lives in a separate bean that reaches this one by
injection.

**3. The cache name carries a version suffix, and it is load-bearing.** Redis stores
the value as JSON. Change the shape of the snapshot record or of the projection
inside it, and a rolling deploy reads entries written by the previous version, fails
to decode them, and answers 500 until the keys expire or are flushed by hand. Bump
the suffix in the same commit as the shape change: a new key is a guaranteed miss,
and the old entries rot away harmlessly.

**4. No `expire-after-write`.** A TTL as a safety net means a missing invalidation
shows up later and looks like something else. The write paths invalidate explicitly;
a missing one is a bug to fix, not to time out.

**5. Invalidation is programmatic, not `@CacheInvalidateAll` on the write
endpoints.** The annotation is the idiomatic choice and was the first
implementation. It also makes every write depend on the cache being reachable:
Quarkus's `RedisCacheImpl.invalidate` has no failure recovery, so with Redis down the
item is created and the response is still a 500 — the interceptor fails after the use
case succeeded. Measured, not assumed: stopping the Redis container turns a create
into a 500 with the annotation, and into a 201 plus a
`cache_invalidation_failed … outcome=write_still_committed` warning without it. A
cache outage must not become a write outage, so invalidation goes through the cache
manager where the failure can be caught.

**6. Reads degrade to the database — but the platform already does most of this.**
This is worth stating precisely, because the obvious version of the claim is wrong.
`RedisCacheImpl.get` recovers by recomputing the value, and its own
`isRecomputableError` lists exactly two failures: `java.net.ConnectException` and a
busy connection pool. A plain Redis outage is therefore handled by the extension,
which logs `Unable to connect to Redis, recomputing cached value` — and the
application-level fallback never fires for it.

What the fallback adds is the failure that is deliberately *not* in that list: a
**decode error**, when a rolling deploy reads entries written by the previous version
of the value type. That propagates, and the endpoint answers 500 until the keys are
flushed by hand — which is exactly the incident that produced decision 3 above.
Versioning the cache name is the fix; the fallback is the seatbelt for the deploy
where someone forgot to bump it. An auth failure after a password rotation and a
command timeout are in the same category.

The lesson generalises past this ADR: before writing a resilience wrapper, find out
what the library already does. Half of this one was redundant, and it took stopping a
container to find out which half.

**7. The cache is not in readiness** (ADR-21). A degraded cache still serves correct
answers; taking the instance out of rotation would turn a slowdown into an outage.

**Paginated queries deliberately do not go through the cache.** Page and size are
caller-chosen, so caching page 7 of a filter nobody repeats fills the cache with
entries that are never read again. That endpoint reads the database directly, and the
split between the two is the actual lesson: cache the small hot projection, not the
query surface.

## Rationale

The stacking is what makes it worth the complexity. The cache removes the database
round trip; the ETag removes the response body. An unchanged poll costs a cache hit
and a 304.

## Alternatives considered

**A TTL instead of explicit invalidation.** Simpler to write and it hides a missing
invalidation behind a delay, which turns a deterministic bug into an intermittent
one.

**One shared cache entry with per-owner filtering after the read.** Cheaper on
memory, and one mistake away from serving one tenant another tenant's rows. The key
is the owner.

**Invalidating the whole cache on every write.** Correct and coarse. Used only for
the one write that crosses owners (the scheduled archive job), where there is no
single key to target.

**Redis for short-window rate limiting.** Not done. A 60-second rate-limit window is
state whose loss is harmless — a restart granting one client one extra request
changes nothing — so it belongs in a bounded in-process map with a hard key cap,
not in a network round trip on every request. The cap matters: an unauthenticated
endpoint where the client picks its own key is a memory-exhaustion vector without
one. Cache and rate limiting look like the same problem and are not.

## Consequences

- Two integration cases pin the behaviour: an unchanged poll answers 304 with an
  empty body, and a write moves the ETag. The second one is the one that catches a
  broken key derivation, where invalidation silently does nothing and the endpoint
  serves stale data indefinitely.
- The cache backend is stated explicitly in configuration rather than inherited from
  which extensions are on the classpath. Reading `quarkus.cache.type` should not
  require knowing that a Redis extension changes the default.
- Cache hit ratio is on the dashboard. A ratio near zero means the invalidation is
  too aggressive or the endpoint is not actually polled — either way the cache is
  not earning its complexity.

## Related

- ADR-21 — why the cache is excluded from readiness
- ADR-10 — why the cached projection is a typed record and a small one
