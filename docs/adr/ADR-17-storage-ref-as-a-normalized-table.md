# ADR-17: The Storage Catalogue Is a Normalized Table

**Status:** Accepted
**Reversibility:** medium — a schema change plus a data migration.

## Context

Every aggregate that holds a file needs the same five facts about it: bucket,
prefix, key, content type, size. Plus a hash and an owner.

The frictionless option is an `@Embeddable` copied into each owning entity. No
join, no extra table, and the ORM handles it.

It also means those columns exist once per owning table, so adding a field to the
blob metadata is a migration per owner. There is no place to catalogue a blob that
has no owner yet, or whose owner has been deleted, which is exactly the state an
orphan-reclaiming sweep needs to be able to see.

## Decision

A single normalized `storage_ref` table, owned by the kernel's storage capability.
Owners reference it by id.

```sql
CREATE TABLE storage_ref (
    id           UUID NOT NULL DEFAULT gen_random_uuid(),
    bucket       VARCHAR(255) NOT NULL,
    prefix       VARCHAR(255) NOT NULL,
    key_name     VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    size         BIGINT,
    hash         VARCHAR(64)  NOT NULL,
    owner_id     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_storage_ref PRIMARY KEY (id)
);
```

No owning entity holds a JPA association to it. Owners store the id as a plain
column and reach the capability through the port, which accepts and returns that
id (ADR-03). That is what keeps a change here from rippling into every owning
aggregate.

The entity has no JPQL queries against it: access is by id, through Panache's
`findById` and `persist`.

An owner that reads the blob's metadata on a hot path may copy the fields it needs
onto its own row — a deliberate denormalization, annotated as such, so a download
does not join into a table its component does not own.

## Rationale

The join is cheap and the flexibility is not: one table means one migration, one
place to add a metric, and a row that can outlive its owner long enough to be
cleaned up.

## Consequences

- One join, or one denormalized copy, on paths that read blob metadata.
- Deleting an owner does not automatically delete the blob. The use case does both
  explicitly, in an order chosen so that a failure leaves an orphaned blob rather
  than a row pointing at nothing.

## Related

- ADR-15 — the port over this table
- ADR-18 — why rows are never shared
- ADR-03 — the id-as-plain-column rule
