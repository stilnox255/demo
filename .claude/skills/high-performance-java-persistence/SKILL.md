---
name: high-performance-java-persistence
description: High-Performance Java Persistence rules from Vlad Mihalcea — Hibernate/JPA fetching strategy (N+1, JOIN FETCH, batch fetch size), entity equals/hashCode via business key, transaction/persistence-context scoping, optimistic vs pessimistic locking, JDBC batching (batch_size, IDENTITY vs SEQUENCE), query shape and pagination, association cascading, second-level cache. Use when writing or reviewing Hibernate/JPA/Panache entities, repositories, or queries. Not for high-level architecture or non-JPA persistence.
---

# High-Performance Java Persistence (Mihalcea)

Binding persistence guideline. When this skill triggers, read the full text and follow it:

**Read `docs/guidelines/high-performance-java-persistence.md` and apply its rules to the current Hibernate/JPA/Panache code.**

Core lens: know the SQL your mapping/query generates before you write it — no N+1,
explicit fetch plans, correct entity equality, transactions and locking sized to real
concurrency, and batching over row-by-row round trips. Run the guideline's review
checklist before considering persistence-layer work done.
