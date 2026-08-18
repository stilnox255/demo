# ADR-40: Remote Deployment via rsync and SSH

**Status:** Accepted
**Reversibility:** high — one script.

## Context

Something has to get the deployment unit onto the host and restart the stack. The
options range from a shell script to a pull-based agent to a full orchestrator.

For a single-host deployment, an orchestrator is a second system to operate. A
pull-based agent is a daemon to keep alive and debug. Both are the right answer at a
different scale, and at this scale the honest description of the work is "copy files
and run two commands".

The script must not be a one-shot, though. A deployment that fails halfway needs to
be re-runnable without reasoning about what already happened.

## Decision

`rsync` over SSH, then `docker compose pull && up -d` over SSH. One script,
restartable at every step.

```
./deploy.sh root@example.com        rolling update
./deploy.sh -f root@example.com     full restart (down first)
```

The properties that make it safe to re-run:

- **rsync with `--delete`** converges the remote copy to match the local one, so a
  file deleted here disappears there. `.env` and `secrets/` are excluded, since they
  are host-generated.
- **`compose up -d` is convergent**, not additive. It recreates only containers
  whose image or configuration changed, so an unchanged database stays up. A
  frontend-only deploy does not restart the backend.
- **No build step.** The images are already built and pushed (ADR-38); this moves
  tags.
- **A health gate at the end.** The script polls the backend's container health and
  fails with a non-zero exit if it does not become healthy, so a broken deploy is
  reported rather than assumed successful. Without it the script's success means
  "the files copied".

`-f` exists for the case where a convergent restart is not enough — a changed
network definition, or a container in a wedged state.

## Rationale

Everything here is a standard tool doing its normal job. The only real design work
is the idempotence, and it comes from choosing tools that are already convergent
rather than from logic in the script.

## Consequences

- Deployment requires SSH access to the host. That is also the incident-response
  path, so it exists anyway.
- No rollback command. Rolling back is deploying an earlier image tag, which the
  environment file already parameterises — and that is a smaller mechanism than a
  rollback feature.

## Related

- ADR-39 — what gets copied
- ADR-38 — what the tags point at
