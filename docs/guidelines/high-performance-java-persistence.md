# OBEY High-Performance Java Persistence by Vlad Mihalcea

## Purpose

This repository follows the practical rules from **High-Performance Java Persistence**:
use Hibernate/JPA deliberately, understand what SQL gets generated, and avoid the
performance and correctness traps that come from treating the ORM as a black box.

All code generation, edits, and reviews involving Hibernate/JPA/Panache entities must
optimize for:
- predictable, minimal SQL per use case
- explicit fetch plans instead of accidental lazy-loading cascades
- correct entity identity, equality, and versioning
- transactions and locking sized to the actual concurrency requirement
- batching over row-by-row round trips

This file is a binding engineering policy: `MUST` is binding, `SHOULD` is a strong
default, and `MUST NOT` is forbidden.

---

## Primary Directive

Every entity mapping and query is a SQL-generation decision. When uncertain, prefer the
mapping/query shape that keeps the generated SQL small, predictable, and index-friendly
over the one that reads most "naturally" in Java.

Before merging any code that adds or changes a query path, know:
- how many SQL statements it issues for N rows (no N+1)
- whether it needs write access (no unnecessary locking/dirty-checking overhead)
- whether it needs full entities or just a projection

---

## Connections & Statements

- MUST use a connection pool sized to `pool_size = Tn × (Cm - 1) + 1` reasoning (Tn =
  max threads, Cm = max concurrent connections per thread) — never leave pool sizing
  as an afterthought default in production config.
- SHOULD enable PreparedStatement caching (`hibernate.jdbc.batch_size` alone is not
  enough) so repeated statement shapes are parsed once by the driver/DB.
- MUST NOT use `Statement`/string-concatenated SQL for anything with a variable input —
  bind parameters, always.

## Fetching: the N+1 rule

- MUST NOT let an association load lazily inside a loop. If a collection or `@ManyToOne`
  is accessed per iteration, either fetch it up front (`JOIN FETCH`, entity graph) or
  restructure into a single query with a projection.
- SHOULD default new `@OneToMany`/`@ManyToMany` associations to `FetchType.LAZY`.
  `EAGER` is a mapping-level decision that cannot be overridden per query and silently
  grows every future query touching that entity — treat it as a code smell.
- SHOULD prefer `JOIN FETCH` (or a `@EntityGraph`) in the specific query that needs the
  association over widening the mapping's default fetch type.
- MUST NOT fetch more than one collection association via `JOIN FETCH` in the same
  query (Cartesian product blow-up). Fetch one collection per query, or use
  `@BatchSize`/`hibernate.default_batch_fetch_size` for the rest.
- SHOULD set `hibernate.default_batch_fetch_size` (e.g. 16–50) as a safety net for
  lazy associations that do get touched in a loop, so Hibernate batches the
  `WHERE id IN (...)` lookups instead of issuing one `SELECT` per row.
- MUST use a DTO projection (constructor expression, `Tuple`, or interface projection)
  for read-only views that don't need managed-entity behavior — do not load full entity
  graphs to render a list or a report.

## Entity Identity & Equality

- MUST implement `equals`/`hashCode` based on a business key (a natural, immutable key)
  for entities that go into a `Set` or get compared before an ID is assigned — never
  use Hibernate-generated `id` alone if the entity can exist transiently, and never
  fall back to default `Object` identity for anything held in a `HashSet`/used as a
  Map key.
- MUST NOT use `@Data`/Lombok-generated `equals`/`hashCode`/`toString` on entities that
  include lazy associations — it triggers unintended proxy initialization and can loop
  infinitely on bidirectional associations.
- SHOULD prefer `@NaturalId` for the stable business key when one exists, and use
  `Session#byNaturalId` for lookups that would otherwise be a `WHERE` query on that key.

## Managing State: Transactions & the Persistence Context

- MUST keep the persistence context (`EntityManager`) scoped to a single
  request/use-case transaction — do not let it span multiple independent business
  transactions ("session-per-conversation" antipatterns).
- MUST NOT call `flush()`/rely on auto-flush to substitute for explicit control over
  when writes happen; understand that every query against a dirty persistence context
  triggers an auto-flush and can hide N+1-shaped write patterns.
- SHOULD favor bulk `UPDATE`/`DELETE` (JPQL bulk statements or native SQL) over
  loading N entities into the persistence context purely to mutate one field on each —
  bulk operations bypass dirty checking and the second-level cache, so use them only
  when optimistic locking / cache coherence for those rows isn't required.
- MUST version entities that are updated concurrently by more than one transaction
  path with `@Version` (optimistic locking). Do not reach for `@Version` cargo-cult on
  every entity — apply it where concurrent writers are a real scenario.
- SHOULD use pessimistic locking (`LockModeType.PESSIMISTIC_WRITE`) only for the
  narrow set of operations where lost updates cannot be resolved by retrying an
  optimistic-lock failure (e.g. sequence/counter allocation) — it blocks readers and
  does not scale like optimistic locking does.

## Batching

- MUST enable `hibernate.jdbc.batch_size` (and `order_inserts`/`order_updates`) for any
  code path that persists or updates a non-trivial number of entities in one
  transaction — without it, bulk `saveAll`-style operations degrade to one round trip
  per row.
- MUST NOT mix identity generation strategies that defeat batching. `GenerationType.
  IDENTITY` forces Hibernate to insert one row at a time to read back the generated key
  — prefer a sequence (`GenerationType.SEQUENCE` with a pooled/`hilo` optimizer) when
  batch inserts matter.
- SHOULD chunk very large batch operations (paginate, or process in fixed-size slices)
  rather than holding thousands of managed entities in one persistence context —
  periodically `flush()` + `clear()` when doing bulk imports.

## Query Shape

- MUST use JPQL/Criteria/native SQL — whichever produces the smallest, most explicit
  query for the use case — over loading entities into Java and filtering/aggregating
  in memory.
- SHOULD push pagination (`setFirstResult`/`setMaxResults`) to the database, and prefer
  keyset/seek pagination over `OFFSET` for large, frequently-paged tables — `OFFSET`
  cost grows linearly with the offset.
- MUST NOT paginate an in-memory `List` that was already loaded in full from the
  database "for simplicity" — that is the N+1 problem's slower cousin.
- SHOULD use native SQL or a projection DTO for reporting/analytics queries instead of
  contorting JPQL/Criteria to express aggregations the ORM isn't built for.

## Associations & Cascading

- MUST NOT cascade `REMOVE` across an association unless the child truly cannot exist
  without the parent (true ownership, not just "usually deleted together").
- SHOULD manage bidirectional association consistency with helper methods on the owning
  side (`addChild()`/`removeChild()` that update both sides) rather than trusting
  callers to keep both sides in sync.
- MUST mark the non-owning side of a bidirectional `@OneToMany`/`@ManyToOne` with
  `mappedBy` — do not let both sides independently manage the foreign key, which
  produces redundant `UPDATE` statements.

## Caching

- MUST NOT enable the second-level cache for an entity/collection without first
  confirming the read/write ratio and staleness tolerance justify it — a cache with a
  high write rate mostly adds invalidation overhead without payoff.
- SHOULD scope second-level cache usage to genuinely read-heavy, rarely-changing
  reference data (lookup tables, catalog data) rather than transactional entities.

---

## Review Checklist

When reviewing or writing code touching Hibernate/JPA/Panache entities, verify:
- [ ] No association access inside a loop without prior `JOIN FETCH`/entity graph/batch
      fetch sizing
- [ ] No more than one `JOIN FETCH`'d collection per query
- [ ] Read-only list/report endpoints use projections, not full entity graphs
- [ ] `equals`/`hashCode` on entities use a business key, not just the DB id
- [ ] Bulk mutations use bulk JPQL/SQL, not a loop over loaded entities, when cache/
      optimistic-lock coherence isn't required
- [ ] `@Version` present where concurrent writers actually exist
- [ ] Batch inserts/updates have `hibernate.jdbc.batch_size` in effect and are not
      defeated by `GenerationType.IDENTITY`
- [ ] Pagination happens in SQL, not in Java after a full load
- [ ] Bidirectional associations are kept consistent via owning-side helper methods
