# ADR-34: Real Dependencies for Integration Tests, as CI Job Services

**Status:** Accepted
**Reversibility:** medium — the alternative is mocking, which changes the tests.

## Context

Integration tests need a database and an object store. Three options:

**An in-memory database.** Fast, and it does not behave like the real one. Partial
indexes, `ON CONFLICT`, and the exact optimistic-lock exception are all things the
tests would then not cover — which are exactly the things worth covering.

**A shared database.** One environment, many test runs, and the failures depend on
what another run left behind.

**A container per run.** Real engine, disposable state.

Container-per-run has a variant question in CI: the framework's own dev services
can start them, or the runner can. Dev services restart their containers on every
framework re-augmentation, and a suite with several test profiles triggers one
re-augmentation per profile — so a full run creates and destroys the database
several times.

## Decision

Real dependencies, in containers, always. Never mocked at the driver level.

- **Locally:** the framework's dev services start Postgres and an S3 emulator and
  stop them with the test run. Nothing to remember, nothing left running.
- **In CI:** the same images as runner-managed job services, started once, in
  parallel with checkout and the dependency cache restore. Dev services are switched
  off explicitly for those jobs.

The trade CI makes: a job service is not disposable per test class, so schema
cleanup becomes the suite's job. That is what the Flyway clean-at-start flags in the
integration job are for, and they are scoped to that job so they can never run
anywhere else.

The S3 emulator is chosen for boot cost: a small native-boot image rather than a
large Python one, because that cost is paid on every spin-up.

## Rationale

The tests that matter here are the ones that assert behaviour the database owns.
Against a substitute they assert the substitute's behaviour.

Moving the containers to job services in CI is purely about the re-augmentation
multiplier, and it is worth the explicit cleanup.

## Consequences

- CI needs a Docker-capable runner. Standard.
- The integration job carries an environment block pointing at the job services.
  That duplication with the dev-services configuration is deliberate: the two are
  different environments and pretending otherwise is what breaks when one changes.
- Test classes must not depend on each other's data, since the database is shared
  within a shard. Sharding proves it.

## Related

- ADR-35 — keeping the same isolation for external services
- ADR-36 — why compose-based dev services stay off in tests
