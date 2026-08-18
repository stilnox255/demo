# ADR-21: Readiness Reflects Core-Serving Capability Only

**Status:** Accepted
**Reversibility:** high — health check annotations.

## Context

A readiness probe answers one question: should this instance receive traffic. It is
tempting to answer a different, easier question — is everything the instance
depends on healthy — because that is what a dependency check naturally measures.

Conflating the two turns a degraded dependency into an outage. An instance that
could still serve most requests correctly is pulled out of the load balancer
rotation; if the dependency is shared, every instance is pulled at once, and a
partial failure becomes a total one.

The opposite error is a readiness probe that always answers UP. It gives the load
balancer nothing to work with and routes traffic into an instance that cannot
serve.

Liveness is a third question again: should this container be restarted. A restart
does not fix an unreachable database, so a liveness probe that checks dependencies
produces a restart loop during someone else's outage.

## Decision

**Readiness** includes only dependencies without which the instance cannot do its
core job:

- the database connection pool — the framework's own check, not a hand-written one
- the object store, since storing and serving files is a core capability here

**Readiness excludes** anything the application can degrade around. The cache is
the explicit case: with the cache unreachable the application still serves correct
answers from the database, just slower (ADR-41). Its client's built-in readiness
check is switched off deliberately, and the reason is written next to the
configuration key.

**Liveness** carries no dependency checks at all. It answers whether the process is
functioning, which is what a restart can fix. The container health check probes
liveness; the load balancer probes readiness.

Neither probe is hand-written where the framework provides one. A custom database
readiness check next to the framework's own means the database is probed twice per
call, and a custom liveness check that returns UP unconditionally is exactly what
an empty liveness endpoint already reports.

## Rationale

The rule that produces the right answer each time: *would taking this instance out
of rotation help?* For a broken database, no — every instance has the same problem
and removing them all serves nobody. For an exhausted local resource, yes.

## Consequences

- A cache outage shows up in metrics and logs, not in the routing. That is
  intentional, and it means the dashboard is how anyone finds out.
- The composition of both probes is pinned by a test, so a future edit cannot
  quietly add a dependency to readiness or reintroduce a duplicate.

## Related

- ADR-41 — the degradation that makes the cache excludable
- ADR-20 — bounding the dependency calls themselves
