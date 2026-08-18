# ADR-43: Capacity Tests Live In-Repo as a Gatling Module

**Status:** Accepted
**Reversibility:** high — one Gradle module.

## Context

Capacity is an assumption until it is measured, and the assumption is usually wrong
in a specific direction: the connection pool, the cache hit ratio, or the thread
pool turns out to be the limit, and nobody knows which until traffic arrives.

Load tests are the standard answer and they routinely fail to exist. The reasons are
consistent: they live in a separate repository that drifts out of step with the API,
or they are an ad-hoc script on someone's laptop that produces a number nobody can
reproduce, or they were run once before launch and never again.

## Decision

A Gatling module in this repository, as its own Gradle subproject.

```
load-test/
├── build.gradle
├── src/gatling/java/simulations/    one class per traffic shape
├── src/gatling/resources/           gatling.conf, logback.xml
└── scripts/                         seed.sql, cleanup.sql
```

Four things make it more than a script:

**1. In-repo, so it moves with the API.** A simulation that references a removed
endpoint fails to compile in the same pull request that removed it.

**2. Scenarios mixed in one run.** Read-heavy polling, paginated reads and writes
run simultaneously, because the interesting failures are at the seams: a write rate
that keeps the cache permanently cold turns the pollers into readers, and no
single-scenario run would ever reveal that.

**3. Assertions, not just a report.** The run fails the task on a failed request, on
a p99 above budget, and on the conditional-GET path exceeding a much tighter budget.
A capacity test that only produces graphs gets run once; one that fails can go in a
pipeline. The tight budget on the 304 path is the assertion that catches a broken
cache.

**4. Seed and cleanup as SQL.** Creating thousands of rows over HTTP takes minutes
and measures the write path, which is not what a read scenario is for. Cleanup
matches on a marker the seed wrote rather than on the owner, so a run against a
shared environment cannot delete rows a person created.

The module is **not** in CI. A load test on a shared runner measures the runner, and
the free tier is not the place to find out about capacity. It is run deliberately,
against a deployed environment, with the target and the rate as parameters.

## Rationale

The cost is one extra Gradle module — the only structural expansion in this
repository — and the guidelines require capacity tests. Without a harness in the
repository they do not get written; that is the observed outcome, not a prediction.

The module stays on an LTS Java rather than the backend's version: nothing in it
compiles against the application, and the plugin's toolchain support lags, so a
version bump in the backend should not block a capacity test.

## Consequences

- Two Gradle projects instead of one. The root project is unaffected; the module is
  only built when its task is asked for.
- A simulation needs an access token, supplied as a parameter. It does not perform
  the OIDC flow itself — measuring the identity provider is a different test.
- Results are a local report. Trending them over time would need somewhere to put
  them, and that is a decision for when there is a second data point.

## Related

- ADR-41 — the cache behaviour the assertions protect
- ADR-32 — why this is not a CI job
