# ADR-44: One Object-Store Emulator for Dev and Test

**Status:** Accepted
**Reversibility:** high — one environment variable and one compose service.

## Context

The application reaches object storage through a port and the AWS SDK (ADR-15), so any
S3-compatible server will do. Deployed environments run RustFS, in `deploy/`.

Locally there were two. The test suite used Floci, provisioned by Dev Services and
chosen for its boot cost (a ~150MB native image, paid per forked test JVM). Development
used RustFS instead, as a service in `compose-devservices.yml` with a named volume, so
uploads survived a dev-mode restart.

That split was not really a choice a developer could make. The compose file's own header
offered "skip it and the Dev Services emulator takes over", but `%dev` carried an
unconditional `quarkus.s3.endpoint-override` pointing at `localhost:9000`. An explicit
override always beats Dev Services, so skipping RustFS did not fall back to anything —
it just broke dev mode. The documented option had never worked.

## Decision

One emulator for both local profiles: **Floci, via Dev Services, in `%dev` and `%test`.**
RustFS stays, in `deploy/` only.

`%dev` carries no `endpoint-override` line. Pointing development at a different store —
a persistent one, or a shared one — is `QUARKUS_S3_ENDPOINT_OVERRIDE` in `.env`. Quarkus
maps the environment variable to the config key without one having to exist in
`application.properties`, and an explicit override beats Dev Services, so that is the
whole mechanism.

## Rationale

Development matches the suite. The failure a developer hits at 5pm is then the failure CI
hits, rather than a second one that only the store used in dev can produce.

The dev stack loses a container, a named volume and the only unpinned `:latest` image in
its path. That last one is worth stating plainly: pinning it was the alternative, and
deleting the thing is cheaper than maintaining a pin for it.

Dev/production parity on the emulator was the argument for RustFS in development, and it
had already been given up for the test suite. What actually differs between S3
implementations — path-style addressing, static credentials, bucket creation — is
exercised identically either way: path-style is on for every profile, and
`S3BucketInitializer` creates the bucket on startup wherever it runs, which is also why a
fresh emulator container needs no init step.

## Consequences

- Uploads in development do not survive a restart. For a starter with no data worth
  keeping that is a fair trade; when it stops being one, the environment variable above is
  the way back and needs no code change.
- A RustFS-specific defect surfaces first in a deployed environment rather than in dev.
  That is the real cost of this decision, and it is the cost the test suite has carried
  since Floci was picked for it.
- `compose-devservices.yml` holds a single service. The all-or-nothing port-bind failure
  mode is unchanged, and the dev proxy is now the only thing that can trigger it.

## Alternatives

**RustFS in development too, pinned.** The parity argument, and it keeps uploads across
restarts. Rejected: it buys parity for the one dependency where the abstraction is
thinnest and the SDK does the talking, and pays with a second emulator to keep working, a
volume to clean up, and a version to track. The upstream tags are still pre-1.0 release
candidates, so `deploy/` now pins one deliberately — carrying that same bump through
development as well is cost without a matching return.

**RustFS everywhere, including tests.** Removes the split entirely, at the price of the
spin-up cost Floci was chosen to avoid, multiplied by every forked test JVM.

**Keep both, and make "skip RustFS" actually work** with a conditional `%dev` override.
More configuration to explain than either single choice, and the branch nobody takes is
the branch that rots.

## Related

- ADR-15 — the port that makes the server interchangeable
- ADR-34 — real dependencies for integration tests
- ADR-36 — what the test profile starts, and what it deliberately does not
