# ADR-04: Transaction Boundaries Belong to Use Cases

**Status:** Accepted
**Reversibility:** high — annotation placement, no schema or contract impact.

## Context

A transaction boundary answers one question: what is the unit of work that must
succeed or fail together. Only the caller knows the answer, and there are three
tempting places to put it that are all wrong.

**On a repository method.** Every write gets its own transaction, so a use case
that writes twice cannot roll both back. Worse, the boundary is now invisible at
the place where it matters.

**In a helper.** A helper that opens a transaction makes its callers' boundaries
depend on an implementation detail one level down, and a second caller inherits a
boundary nobody chose for it.

**Around everything.** A transaction that spans a remote call holds a database
connection for the duration of the network round trip. Twenty slow clients then
exhaust a pool of twenty connections, and the symptom is a database that looks
overloaded while doing nothing.

## Decision

`@Transactional` goes on the use-case class method. Nowhere else.

For a flow that needs a remote call between reading and writing:

- Each database step is its own use case with its own in-port and
  `@Transactional(REQUIRES_NEW)` on the service method. `REQUIRES_NEW`, not the
  default: an orchestrator may itself be called from a transactional context, and
  `REQUIRED` would silently enrol every step in that one.
- The class sequencing the steps is an orchestrator and is **never**
  transactional. The remote calls happen there, with no transaction open.
- A step re-reads the aggregate it writes. Passing in an instance loaded before
  the remote call would discard whatever changed in between.
- A batch loop uses one transaction per item, so one failure cannot discard the
  work already done.

A step that needs its own boundary lives in its own bean, not in a private method.
`@Transactional` is an interceptor: a self-call does not pass through the proxy,
so the annotation on a private method silently does nothing. A safeguard that
cannot fire is worse than no safeguard, because it also stops anyone from looking.

## Rationale

Splitting a use case is the cheap half of this decision. The expensive half is
resisting the single large transaction, which is always the smaller diff and
always the one that surfaces as a pool exhaustion under load.

## Consequences

- More use-case classes for flows that touch a remote system mid-write.
- No `QuarkusTransaction` in the application layer, no transaction boundaries on
  repository ports, and no callback-shaped port signatures (`mutate(id, Consumer)`)
  used to smuggle a boundary in.
- The orchestrator has no rollback across its steps. Compensating for a partial
  failure is explicit work, which is the honest representation of what a
  distributed step sequence actually is.

## Related

- ADR-01 — one class per use case, which makes the boundary a class-level fact
- ADR-20 — bounding the remote calls that happen outside the transaction
