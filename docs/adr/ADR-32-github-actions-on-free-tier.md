# ADR-32: GitHub Actions on the Free Tier as CI Platform

**Status:** Accepted
**Reversibility:** medium — the workflows are platform-specific YAML.

## Context

The project needs CI. The realistic options are the platform hosted next to the
repository, a self-hosted runner on the same machine that already runs the
deployment, or an external service.

A self-hosted runner is tempting because the machine exists: no minute limits, and
the Docker daemon is right there. It also means CI shares a host with production, so
a runaway test run competes with live traffic, and a compromised pull request
executes on the deployment host.

## Decision

GitHub Actions on the free tier, with the constraint treated as a design input
rather than something to work around.

What that constraint produces, all of which are good properties anyway:

- **Path-filtered jobs** (ADR-33), so a frontend change does not pay for the
  backend suite.
- **A split test topology** — lint, unit tests and integration tests as separate
  jobs — so the fast feedback arrives in seconds rather than behind the slowest
  part.
- **Sharded integration tests**, which is the only way to make a Quarkus suite
  finish quickly, and which also proves the suite has no order dependencies.
- **Artifacts promoted rather than rebuilt** (ADR-38), which halves the container
  work and gives a stronger guarantee.
- **Actions pinned to a commit SHA**, not a tag. A tag is mutable, and a
  third-party action with write access to the pipeline is the most attractive
  supply-chain target in a repository.

If the minutes ever run out, the answer is a self-hosted runner on a machine that
is not the production host — not a rewrite of the pipeline.

## Rationale

The free tier is enough for this size of project, and the discipline it forces
produces a pipeline that is fast and cheap on any platform.

## Consequences

- Playwright end-to-end tests are not in CI: they boot the full stack including the
  identity provider. That is a deliberate manual step, and it is written down as one
  rather than left as a gap.
- Every action reference carries a SHA and a version comment, so an update is a
  visible diff.

## Related

- ADR-33 — the triggers and filters
- ADR-34 — how integration tests get their dependencies
- ADR-38 — what the pipeline ships
