# ADR-35: Fixed, Distinct Ports for Test Dependencies

**Status:** Accepted
**Reversibility:** high — configuration.

## Context

Dependencies started for a test run need ports. Random ports avoid collisions but
make a failure hard to inspect: by the time you want to connect to the database and
look, the run is over and the port is gone.

Fixed ports are inspectable and collide — with a development stack running on the
usual port, and between two concurrent runs on the same machine.

The specific failure that fixed ports cause is worth naming, because it is silent:
a test run that connects to the *development* database instead of a fresh one
passes, mutates real local data, and leaves the developer's environment in an
unexplained state.

## Decision

Each environment gets its own fixed, distinct port, and the environments never
share one:

- **Development** uses the conventional ports, so tooling connects without
  configuration.
- **CI** uses runner-managed job services on their own ports, supplied to the tests
  by environment variables (ADR-34).
- **Local test runs** get their ports from the dev services, which pick a free one
  per run rather than reusing the development port.

The environment variables in the CI job are set explicitly rather than inherited, so
a test run there cannot accidentally resolve to something else.

The rule that makes this hold: **a test never falls back to a default connection
string.** A missing configuration is a failure, not a reason to try the conventional
port. The failure mode this prevents is a test suite quietly succeeding against the
wrong database.

## Rationale

Inspectability is worth real money during a flaky-test investigation. The collision
risk is handled by keeping the environments separate rather than by randomising, and
by making an unconfigured connection fail rather than guess.

## Consequences

- Running the suite while a development stack is up works, because the two use
  different ports.
- Two concurrent local runs can still collide. Accepted: the fix is not running two,
  and the failure is loud.

## Related

- ADR-34 — where these dependencies come from
- ADR-36 — the compose-based services that stay off by default
