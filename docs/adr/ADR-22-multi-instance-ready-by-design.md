# ADR-22: Multi-Instance Ready by Design

**Status:** Accepted
**Reversibility:** low — retrofitting it means finding every piece of local state.

## Context

The application runs as a single instance and will for a long time. That makes
in-memory state extremely tempting: a map of pending work, a cached lookup table, a
counter, a lock. Each one works perfectly and each one silently makes a second
instance impossible.

The bill arrives all at once, at the worst time: the day one instance is not
enough, or the day a rolling deploy briefly runs two. The symptoms are not a clean
failure but duplicated scheduled work, a lost update, and a cache that disagrees
with itself.

## Decision

No instance-local state that affects correctness. Concretely:

- **Shared state lives in the database or the cache**, never in a field.
- **No in-memory locks.** Coordination is optimistic locking on a version column
  (a conflict is detected and reported) or a database-level guard.
- **Scheduled work is idempotent and bounded**, so two instances running the same
  tick produce the same end state as one. It does not assume it is the only runner.
- **Sessions are stateless.** Authentication is a bearer token validated per
  request; there is no server-side session to pin a client to an instance.
- **Uploads go straight to the object store**, so a subsequent request served by
  another instance can read the file.

In-memory state is allowed where losing it is harmless and the alternative costs
more: a short-window rate-limit counter is fine per instance, because a restart
granting one extra request changes nothing that matters. That decision gets written
down where it is made, not assumed.

## Rationale

Every item on that list is nearly free while writing new code and expensive to
retrofit, because retrofitting means finding all of them — and the ones that matter
are the ones nobody remembers.

Stating it as a decision is what makes it reviewable. "Where does this state live?"
is a question with a right answer, not a preference.

## Consequences

- Some operations that would be trivial with a local lock need a version column or
  a conditional update instead.
- A second instance can be started without an audit.
- Scheduled jobs are written to tolerate being run twice, which also makes them
  safe to trigger by hand during an incident — the reason each one has an
  administrative endpoint.

## Related

- ADR-41 — the cache as shared rather than local state
- ADR-04 — optimistic locking as the coordination mechanism
