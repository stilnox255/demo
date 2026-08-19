# ADR-47: Owner Scoping Is the Default Read Scope

**Status:** Accepted
**Reversibility:** high before the first query exists, low afterwards — the scope
sits in every port signature and in what a row is taken to mean.

## Context

An id in a path plus an ownership check in the caller is how an IDOR ships. The
check compiles, the tests pass, and the endpoint hands out somebody else's row.

The demo component avoids that by putting the scope into the port: every read takes
an `ownerId`, and there is no `findById(UUID)` to reach for. Owner scoping is the
narrowest scope there is (one principal owns its rows) and the only one that is
correct without a membership model, which is what makes it the right choice for a
repository that has no business domain in it.

Most products are not owner-shaped. Rows shared by a team or a whole tenant are the
common case, and the rule as written in `src/CLAUDE.md` and in
`DemoItemRepository` reads as an invariant rather than as the narrow default it is.
That matters most for the reader who is not a person: an agent treats the rule as
binding and reproduces it in a domain whose rows were never meant to have a single
owner.

## Decision

Owner scoping is the default scope, not an invariant. Three things are invariant,
and only these three:

1. **The scope is an argument of the port.** No unscoped `findById(UUID)`, and no
   `.filter()` after a `list()`. A scope that is a parameter cannot be forgotten; a
   scope that is a convention can.
2. **The scope is a server-side fact.** It comes from `SecurityIdentity`, never from
   a request body or a path parameter (ADR-45).
3. **An exception carries a name that looks like one.**
   `findByIdForSignedAccess(UUID)`, so that using it looks like the exception it is.

What varies per project is *what* the scope is: a principal, a team, a tenant. A
product with shared data replaces the argument at the port, renames `*ForOwner` to
`*ForTeam` or `*ForTenant` so the signature still states the rule, and lets the
compiler find every caller. Deciding this before the first query exists costs an
afternoon. Deciding it once there is data costs a migration of every row and of
every query that reads one.

## Alternatives considered

**Unscoped ports, authorization in the use case.** The conventional layering answer,
and the one this repository rejects: a missing check is invisible in review, because
there is nothing on the page to notice.

**A generic `Scope` type in the kernel, ready for tenants.** Rejected twice over. It
is speculative, and a scope describes something the business cares about, which puts
it below the kernel's bar (ADR-02).

**Row-level security in PostgreSQL.** A real option for tenant-shaped products, and
the strongest of the three, because it also holds for the query nobody reviewed. It
costs a session variable set per request, and it moves the rule out of the code the
test suite covers.

## Rationale

The escapes already exist. `findByIdForSignedAccess` serves the token-authorized
download (ADR-19), and `findStaleDrafts` reads across owners because the archive job
has to. Two named exceptions in one small component are the evidence: this is a
default with documented holes, not a property of the system.

There is deliberately no fitness function for it. ArchUnit sees packages and types;
it cannot see whether a `String` parameter means an owner. And a rule meant to be
reversed per project does not belong in a build that fails when somebody reverses
it. The pin belongs on the mechanism (the scope is a parameter), not on the value it
happens to carry here.

## Consequences

- The port signatures document who may read what. `DemoItemRepository` says the
  model is single-owner without anyone opening a use case.
- A scope change is a signature change, so it is loud. That is the intent and also
  the cost: no configuration switch turns owner scoping into tenant scoping.
- Cross-scope reads are allowed and exist. They are named, and the name is the
  review surface.

## Related

- ADR-01 — the layering that puts the port in the application layer
- ADR-19 — the signed-download path, and why it needs an unscoped lookup
- ADR-45 — where the identity behind the scope comes from
- ADR-02 — why the scope does not become a shared kernel type
