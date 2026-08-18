# ADR-03: Cross-BC Persistence Decoupling

**Status:** Accepted
**Reversibility:** medium — undoing it means reintroducing the JPA associations it removes.

## Context

Two business components in one database will eventually want each other's data.
The obvious mechanism is a JPA association: a `@ManyToOne` from one BC's entity
to the other's. It is one annotation, the ORM does the rest, and the query is
efficient.

It also welds the two together. The association means one BC's entity class
imports the other's, so neither compiles alone, a fetch strategy chosen in one
changes the query plan in the other, and a schema change on one side breaks the
other's mapping. What looked like a read turns into shared ownership.

## Decision

Two mechanisms, and no third:

1. **A denormalized foreign key plus the fields actually read.** The owning side
   stores the other side's id as a plain column, and copies the handful of fields
   it needs on the hot path. No JPA association, no import of the other BC's
   entity.

2. **A published query in-port.** When a component needs live data rather than a
   copy, it calls `{otherBc}.application.port.in.{Noun}QueryPort`. The port
   returns the other BC's domain types or a purpose-built view — never its JPA
   entities.

Which one to use follows from the question being asked: a value that must be
correct *now* goes through the port; a value that describes the state at the time
of writing gets copied.

## Rationale

Denormalization is usually argued for on performance grounds. That is not the
argument here — it is a coupling decision that happens to also be faster. The
copy is a fact recorded at a point in time, which is often what the reader
actually wanted: the name the file had when it was attached, not the name it has
today.

The trade is explicit: copied data can go stale. That is acceptable exactly when
the value is a historical fact, and unacceptable when it is not, which is why the
port exists as the second mechanism rather than as a fallback.

## Consequences

- No JPA association crosses a BC boundary. Enforced by ArchUnit
  (`adapterOutDoesNotDependOnAnotherBcsAdapterOut`).
- Copied columns need a comment saying why they are copies, or the next reader
  removes them as redundant.
- A component can be extracted into its own service without unpicking mappings.
- Referential integrity across the boundary is not enforced by the database. A
  dangling id is possible and has to be handled where it is read.

## Related

- ADR-01 — the boundary this decision protects
- ADR-17 — the storage catalogue, whose id is referenced exactly this way
