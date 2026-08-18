# ADR-07: No API Versioning via URL Path

**Status:** Accepted
**Reversibility:** high — a version prefix can be added later; the reverse is the hard direction.

## Context

`/api/v1/...` is the reflex. It looks like cheap insurance and it is nearly free
to add on day one.

The cost arrives later. A version in the path is a promise that v1 keeps working
after v2 ships, which means two code paths, two sets of tests and two sets of bugs
— for as long as any client remains on v1, which in practice is forever. Most
projects that add the prefix never ship a v2 at all, so they pay the noise in
every path and get nothing.

## Decision

No version segment. The API evolves compatibly:

- **Adding** a field, an endpoint or an optional parameter is not a breaking
  change and needs no version.
- **Removing or renaming** a field is done in two steps: add the new one, mark the
  old one deprecated in the OpenAPI document, remove it after the clients have
  moved.
- **A genuinely incompatible change** — different semantics for the same
  operation — gets a new resource path, not a new version of the old one. That is
  narrower: only the affected endpoint carries the old and new shape, rather than
  the whole surface.

If a hard version boundary is ever needed, it arrives as a header
(`Accept: application/vnd.starter.v2+json`) or as a separate deployment. Both are
reversible; a path prefix baked into every client URL is not.

## Rationale

The deciding question is what happens if this decision is wrong in each
direction. Wrong without a prefix: add one, and old clients keep working because
their unprefixed paths can be kept. Wrong with a prefix: every client URL,
bookmark and integration carries the version forever.

Compatible evolution is also a discipline rather than a technique. Naming it here
means "will this break a client" is a question asked in review, which is where it
is cheap.

## Consequences

- Every change is reviewed for client compatibility. That is the work this
  decision buys, and it is work either way — a version prefix just lets it be
  deferred until there are two versions to maintain.
- Deprecation lives in the OpenAPI document, so a client can see what is going
  away without reading a changelog.

## Related

- ADR-05 — the paths this decision keeps clean
- ADR-11 — where a deprecation is announced
