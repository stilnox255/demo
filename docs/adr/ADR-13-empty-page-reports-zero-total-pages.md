# ADR-13: An Empty Page Reports Zero Total Pages

**Status:** Accepted
**Reversibility:** high — one expression.

## Context

A paginated response carries `totalPages`. For an empty result there are two
defensible answers, and the failure mode is having both.

That is what happens when the number is computed in more than one place: the
application-layer page type special-cases the empty result to `1` because "there
is always a first page", while the wire representation computes
`ceil(totalItems / pageSize)` and reports `0`. Which number a client sees then
depends on which endpoint it called. Worse, a frontend absorbs the inconsistency
with `meta?.totalPages || 1` — and that guard makes the divergence invisible,
which is why it survives.

## Decision

`totalPages` is `ceil(totalItems / pageSize)`, everywhere. An empty result reports
`0`.

There is one formula, one static method, and the wire representation and the
application-layer type both call it. `totalPages` is **not** stored as a field: a
stored derived value lets a caller construct a page that claims a count
inconsistent with its own contents, and be believed.

## Rationale

Spring Data settles the default question for a JVM codebase: an empty paged result
returns `0`. Django and Laravel clamp to `1` on the grounds that there is always a
first page to render. Both are defensible; the JVM convention is the one a reader
here will expect.

Zero is also cheaper at both ends. The formula needs no branch, and a client
looping `for (p = 1; p <= totalPages; p++)` iterates zero times over an empty
result where `1` would send it after a page it already knows is empty.

The frontend argument for `1` does not survive inspection: `meta?.totalPages || 1`
guards an undefined `meta` during loading just as much as it guards a zero, and a
component reading `getAttribute("total-pages") || 1` needs a default for an absent
HTML attribute regardless. Those guards stay whatever the backend sends.

## Consequences

- `meta.totalPages` is `0` for an empty result on every paginated endpoint. One
  behaviour instead of two.
- Removing the stored field is what makes it stick: the compiler points at every
  call site that used to pass the value in by hand, so none can keep asserting a
  stale number.

## Related

- ADR-06 — the parameter names on the request side
