# ADR-18: One Catalogue Row per Upload, Never Shared

**Status:** Accepted
**Reversibility:** high — it is the absence of a mechanism.

## Context

The catalogue records a content hash for every blob. Once that is there,
deduplication is an obvious next step: two uploads of the same bytes could share
one row and one object, saving storage and an upload.

It is a trap, and the shape of the trap is ownership. If two owners share a row,
the first delete removes the object both of them point at. The second owner's
download then fails, in an account that did nothing — and the failure surfaces far
from the delete that caused it.

Refcounting is the usual answer, and it turns a simple delete into a
read-modify-write under concurrency, in a table that is written by every upload.
The cost is a class of bug (a leaked refcount leaks storage forever; a
double-decrement deletes live data) traded against saving some disk.

## Decision

Every upload gets its own catalogue row and its own object. No sharing, no
refcount.

The hash is still recorded, and it is still useful: it identifies duplicate
*content*, so a caller can be told "you already uploaded this" or a report can
count distinct bytes. It is deliberately not unique, and it does not make two rows
into one.

## Rationale

Storage is the cheapest resource in this system. Correctness under concurrent
delete is not, and refcounting puts the hard part on the delete path — the one
that runs when a user is already annoyed.

The saving would also be smaller than it looks: duplicate uploads are rare outside
of specific workloads, and the ones that exist are usually small.

If deduplication ever becomes necessary, the right shape is a separate content
table with an explicit refcount and its own concurrency story — not a unique index
on this table.

## Consequences

- Two uploads of identical bytes occupy twice the space. Accepted.
- Delete is a single-row operation with no coordination.
- An integration test asserts that identical bytes get separate rows, for both the
  same owner and two different owners. Without it, someone adds the unique index
  as an optimisation.

## Related

- ADR-17 — the table
- ADR-15 — the port whose delete stays simple because of this
