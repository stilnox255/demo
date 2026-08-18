# ADR-24: Configuration Exclusively via Environment Variables

**Status:** Accepted
**Reversibility:** high per key.

## Context

A configuration value that differs per environment has to come from somewhere. The
options are a baked-in profile file, a mounted configuration file, or the
environment.

Baked-in profiles mean the artifact differs per environment, so what runs in
production is not what was tested. Mounted files mean a deployment has to manage a
file per environment, and drift between them is invisible until something behaves
differently in one place.

## Decision

Every value that differs per environment is read from the environment, with a
development-friendly default written as `${SOME_VAR:default}` in the configuration
file. One artifact, configured at start.

The rules that make it work:

- **A production-critical value has no default.** The signing secret for download
  tokens is `${DOWNLOAD_TOKEN_SECRET}` with nothing after the colon, so a
  deployment that forgets it fails at startup instead of running with a value
  anyone could guess. Fail fast at boot, not at the first request.
- **Secrets arrive as files, not as values.** A password in an environment
  variable is visible in `docker inspect` and in the process environment. The
  deployment mounts a secret file and the entrypoint reads it into the variable the
  application expects, stripping the trailing newline that a generated file carries
  — that newline is invisible in every editor and makes a database reject the login.
- **Values that are the same everywhere stay in the configuration file**, where
  they can be reviewed once and carry a comment. Pushing them into the environment
  too would scatter the reasoning across deployment scripts.
- **A value that has to be assembled** — a connection URL containing a password —
  is assembled in the entrypoint, with reserved characters percent-encoded. A
  generated password containing `@` in an unencoded URL points the client
  somewhere else entirely.

## Rationale

This is the twelve-factor answer, and the two additions are the ones that
experience adds to it: no default for a security-critical value, and secrets as
files rather than variables.

## Consequences

- The entrypoint script is part of the deployment surface and does real work. It
  fails loudly with a specific message when a secret file is unreadable, because
  the common cause — a file mode the container's UID cannot read — otherwise
  surfaces as an authentication error somewhere else entirely.
- `.env.example` is the list of what an environment must provide, and is kept in
  step with the compose file.

## Related

- ADR-39 — the deployment unit that supplies these values
- ADR-12 — one specific key and why it cannot be derived
