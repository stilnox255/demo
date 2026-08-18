# ADR-02: Kernel Scope Narrowing

**Status:** Accepted
**Reversibility:** medium — moving a type out of the kernel touches every caller.

## Context

A shared package is the path of least resistance for anything two components
both need. It is also the path of least resistance for anything one component
needs and another might: over time "shared" becomes the place where code goes to
avoid a decision about who owns it.

The failure mode is not size. It is that a shared package with business meaning
in it couples every component to every other component's vocabulary, and nobody
can change the shared type without a survey of the whole codebase.

## Decision

`kernel` holds only cross-cutting primitives with no business meaning:

- pagination types and the single page-count formula
- the problem-details representation
- content hashing
- signed download tokens
- transport-agnostic upload descriptors
- the object-storage port with its S3 and persistence adapters

The bar for adding something: **at least two business components need it, and it
carries no business meaning.** A type that describes something the business
cares about belongs to the component that owns that concept, and reaches other
components through a published in-port (ADR-03).

Access is through the port interface, not the concrete bean. Code outside a
kernel subpackage depends on `ObjectStoragePort`, not on the adapter that
implements it. Interplay inside one subpackage — an adapter and the client it
wraps — stays free.

## Rationale

A bypassed port is invisible in review: the injection compiles, the tests pass,
and the abstraction is simply gone. So the rule is enforced by a fitness function
rather than by attention (`kernelCapabilitiesAreOnlyAccessedThroughPorts`).

The "two components need it" bar is the Rule of Three applied one step earlier
than usual, because the cost here is not duplication but coupling: a type in the
kernel is a type every component may reach for.

## Consequences

- Some duplication across components is accepted. Two similar value objects in
  two BCs are cheaper than one shared type that pins both.
- The kernel has no business vocabulary in it, which is what makes it safe to
  depend on from anywhere.
- A new shared type is a conversation, not a commit.

## Related

- ADR-01 — the layering the kernel sits beside
- ADR-15 — the object-storage port, the kernel's largest capability
