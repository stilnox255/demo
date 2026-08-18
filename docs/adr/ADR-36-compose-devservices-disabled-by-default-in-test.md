# ADR-36: Compose-Based Dev Services Off by Default in Tests

**Status:** Accepted
**Reversibility:** high — one configuration key.

## Context

The framework can start a compose file's services alongside the application in
development. That is useful for the things it cannot provision itself.

In tests it is a liability. The compose file contains services a test run does not
need, each with its own image pull, boot time and health check — paid per forked
test JVM. And when one of those services is unavailable or its image has moved, an
unrelated test suite fails at startup with an error about a container.

The same reasoning applies to any dependency whose extension starts a container by
default. A caching extension on the classpath will happily start a cache container
for a suite that runs the in-process backend, and every test class pays for a
container none of them uses.

## Decision

Compose-based dev services are **off in the test profile**, explicitly. So are the
container-starting dev services for dependencies the tests do not exercise —
the identity provider and the cache backend, each switched off with the reason next
to the key.

Development keeps them on. What tests get instead is the minimum set: a database and
an object-store emulator (ADR-34).

Authentication in tests is a test-security annotation supplying an identity
directly, not a real token from a real provider. The provider's own behaviour is not
what the tests are asserting, and starting it to obtain a token means every test
class waits for a realm import.

## Rationale

A test suite should start the dependencies it asserts against and nothing else.
Every additional container is boot time multiplied by the number of forked JVMs, and
one more thing that can fail for reasons unrelated to the code under test.

Switching them off explicitly, with a comment, is what stops the next person from
turning them back on to fix a symptom.

## Consequences

- The test configuration file carries a short list of "off, because …" entries. That
  list is documentation of what the suite deliberately does not exercise.
- Behaviour that genuinely needs a switched-off dependency re-enables it in its own
  test profile, so exactly one context pays for it.

## Related

- ADR-34 — what the tests do start
- ADR-41 — the cache, whose test profile is the example of the exception
