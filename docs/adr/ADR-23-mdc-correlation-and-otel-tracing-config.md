# ADR-23: MDC Correlation and Explicit Tracing Configuration

**Status:** Accepted
**Reversibility:** high — filters and configuration.

## Context

A log line without correlation is nearly useless in an incident. "Failed to load
item" tells you it happened; it does not tell you which user, which request, or
what else that request did. Reconstructing a request from timestamps works while
there is one user.

Distributed tracing solves part of this, and brings its own trap: the tracing
exporter defaults to sending spans to a collector at a well-known local address.
With no collector there, the exporter retries in the background forever and logs
about each failure — so an observability feature that is not fully set up becomes a
source of noise and CPU.

## Decision

**Correlation into the logging context.** A request filter puts the authenticated
user id into the MDC, and the tracing integration puts the trace and span ids
there. The console format carries all three on every line:

```
%d{HH:mm:ss} %-5p [userId=%X{userId} traceId=%X{traceId} spanId=%X{spanId}] (%t) %s%e%n
```

An anonymous request gets no `userId` key at all rather than a placeholder: an
empty value in a log query is indistinguishable from a missing one, and "no user"
is a fact worth being able to filter on.

**Authentication is lazy** — `quarkus.http.auth.proactive=false` — and that is what
makes the filter's job possible rather than harmful. Every path is permit-all at the
HTTP layer, with `@RolesAllowed` doing the real gating. Under proactive authentication
the framework validates any `Authorization` header up front, so a stale or malformed
token turns a public request into a 401 before it reaches the endpoint; the
signed-download path, which authorizes by a token in the URL and ignores the header,
would break for anyone whose browser still holds an expired one. Lazy means resolving
the identity is what triggers validation, so the filter has to resolve it defensively:
an `AuthenticationException` there is treated exactly like an anonymous request, and
authorization stays where it belongs, on the endpoints that declare it.

**Propagation is pinned explicitly** to `tracecontext,baggage` rather than
inherited. Propagation is part of the contract with callers, so it should not
change because a library default changed.

**The exporter is switched off where there is no collector** — development and
tests. That is an explicit `none`, with the reason next to it, not an omission.

**Structured output goes to the log pipeline, plain text to the console** (ADR-24
covers how the endpoint is configured). Two sinks, because they have two different
readers: `docker compose logs` is read by a person, the shipped copy is read by a
query engine. One JSON console would make the first unreadable; one plain-text sink
would make the second unqueryable.

Shipping goes over UDP. Log delivery must never apply backpressure to a request
thread because the collector is briefly down — best-effort is the right trade for
logs, and the wrong one for the request being logged.

## Rationale

The MDC filter is a few lines and turns every log line into something joinable.
The explicit exporter setting is the kind of configuration that only looks
redundant until an environment without a collector spends a week retrying.

## Consequences

- Both the console format and the exporter settings are pinned by a
  configuration-shape test, with the reason in each assertion message — so the
  values cannot drift back silently.
- The MDC is cleared per request by the filter's own lifecycle. A leaked MDC entry
  attributes one user's id to the next request on that thread, which is a
  privacy bug, so the clearing is not optional.

## Related

- ADR-14 — the log levels these lines carry
- ADR-24 — where the collector address comes from
