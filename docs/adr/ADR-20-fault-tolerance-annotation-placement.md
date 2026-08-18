# ADR-20: Fault Tolerance Annotation Placement and Timeout Layering

**Status:** Accepted
**Reversibility:** high — annotations and configuration keys.

## Context

Every outbound call needs a time limit, and some need a retry. MicroProfile Fault
Tolerance provides both as annotations, which raises the question of where they go
— and the intuitive answer is wrong in two ways.

**Placement.** Putting them on a client *interface* looks natural. But circuit
breaker state is kept per intercepted bean instance, so if the client proxy is
constructed per call, each call gets its own breaker, the state never accumulates,
and the breaker can never trip. It looks configured and does nothing. A fallback
method has a related problem: it must live in the same bean as the annotated
method, and an interface cannot hold the injected collaborators a fallback needs.

**Retry safety.** A retry is only correct if the operation can be repeated. For an
upload it cannot: by the time the failure surfaces the request body has already
been consumed, so a second attempt sends a truncated body — and reports success.
Retrying a non-replayable request is worse than failing it.

## Decision

**1. Annotations go on the application-scoped adapter class**, never on a client
interface. The adapter is a singleton, so interceptor state accumulates across all
callers for the lifetime of the application, and a fallback method can sit next to
the collaborators it needs.

**2. Retry only where the operation is repeatable.**

| Operation | Timeout | Retry | Why |
|---|---|---|---|
| Upload bytes | yes | **no** | the stream is consumed; a retry uploads a truncated body |
| Download | yes | yes | idempotent, returns a fresh stream |
| Delete | yes | yes | idempotent |

Retries are bounded, with a delay **and jitter**. Without jitter, every client that
failed at the same moment retries at the same moment, and the retry storm is worse
than the original failure. Retries are also restricted to transport-level
exceptions: retrying a "no such key" burns the budget to get the same answer three
times.

**3. Transport timeouts and method timeouts coexist.** The client's own
connect/read timeouts fire when the socket stalls; the fault-tolerance timeout
bounds the method and triggers the interceptor chain, which is what lets a retry
or a breaker react. The method timeout is set at or above the transport read
timeout for the same client, so whichever fires first still lands in the fault
tolerance chain.

**4. Values live in configuration, not in annotation literals.** Keys are
`<fqcn>/<method>/<Annotation>/<parameter>`, so a timeout can be retuned during an
incident without a rebuild.

## Rationale

Each rule here exists because the obvious alternative fails silently. A breaker
that never trips, a retry that corrupts an upload, and a timeout that needs a
release to change are all things that look correct in review.

## Consequences

- Adding an outbound dependency means adding its keys to the configuration file,
  where the whole timeout budget can be read in one place.
- An annotation with no configured value falls back to the specification default,
  which for a timeout is one second — so a missing key fails loudly rather than
  silently disabling the protection.

## Related

- ADR-04 — remote calls happen outside transactions
- ADR-21 — what a dependency failure does to readiness
