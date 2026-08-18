# ADR-33: Path-Filtered Jobs, PR and main Triggers Only

**Status:** Accepted
**Reversibility:** high — workflow configuration.

## Context

The naive trigger set is "every push to every branch". A developer pushing five
commits to a branch with an open pull request gets ten runs — five for the push and
five for the pull request — with identical results.

The naive job set is "always run everything". A one-line change to a README then
runs the full integration suite.

## Decision

**Triggers:** pull requests, and pushes to `main`. Nothing else. A branch without a
pull request is not ready for a verdict, and a branch with one is already covered.

**Concurrency:** superseded runs on a pull-request branch are cancelled; runs on
`main` never are, so every merge commit keeps one completed run as an audit trail.

**Path filters:** a first job classifies the change, and each subsequent job runs
only if its area was touched.

```yaml
backend:  ['src/**', 'build.gradle', 'settings.gradle', 'gradle/**', 'config/**', '.github/**']
webapp:   ['web-app/**', '.github/**']
```

`.github/**` appears in **every** filter, and that is the load-bearing detail: a
change to the pipeline itself has to run everything, or the change to the pipeline
is untested.

## Rationale

Each rule removes runs that produce no information. The cancellation asymmetry
between branches and `main` is the one non-obvious choice: on a branch the newest
commit is the only one anyone cares about, while on `main` a cancelled run leaves a
merge commit with no verdict.

## Consequences

- A pull request touching only documentation runs the change-detection job and
  nothing else, in seconds.
- A filter has to be extended when a new top-level directory appears, or that
  directory is never verified. The filter list is short enough to review.

## Related

- ADR-32 — why the minutes matter
