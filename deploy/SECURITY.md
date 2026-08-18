# Deployment hardening

What is set, why, and what is deliberately left open. The last part is the important
one: an undocumented gap looks like an oversight, and the next person either
"fixes" it and breaks something or assumes it was considered when it was not.

## Container hardening

| Setting | Applied to | Why |
|---|---|---|
| `security_opt: no-new-privileges:true` | every service | a process inside cannot gain privileges through a setuid binary |
| `cap_drop: [ALL]` | backend, postgres, redis | the default capability set is far wider than any of them needs |
| `cap_add: [SETUID, SETGID, DAC_OVERRIDE]` | postgres, redis **only** | their entrypoints start as root and drop to a service user; the root process uses these for real |
| **no** `cap_add` | backend | it runs as non-root UID 1001, and a capability in the bounding set does nothing for a non-root process without file capabilities on the binary — the JVM ends up with an empty effective set either way |
| `init: true` | backend, frontend, rustfs | a PID 1 that reaps children and forwards signals, so `docker stop` is a clean shutdown rather than a timeout and a SIGKILL |
| `ulimits.nofile: 65536` | backend | the default is low enough that a connection-heavy moment hits it, and the failure looks like a network problem |
| `expose`, never `ports` | everything | nothing is reachable from outside the host except through Traefik. A published port is a decision, and the default is no. |
| `deploy.resources.limits` | every service | one container cannot starve the rest of the host. A limit is also what makes the JVM's `MaxRAMPercentage` meaningful. |
| log rotation (`max-size`, `max-file`) | every service | unrotated container logs fill the disk, on the machine that also serves production |

## Secrets

- Docker secrets from files, never environment variables in the compose file: a
  password in the environment is visible in `docker inspect` and in the process
  environment.
- `mode: 0444` on the mount states the intent. Compose outside Swarm often ignores
  it, which is why the host files are `0644` inside a `0700` directory — see the
  reasoning in `setup-secrets.sh`.
- Entrypoints read the file and strip the trailing newline. A generated secret carries
  one, it is invisible in every editor, and Postgres rejects the login because of it.
- The entrypoint **fails loudly** with a specific message when a secret file is
  unreadable, rather than booting into a confusing authentication error later.
- No secret has a production default. The download-token signing key is
  `${DOWNLOAD_TOKEN_SECRET}` with nothing after the colon, so a deployment that
  forgets it fails at startup instead of minting forgeable tokens (ADR-24).

## Network exposure

- Redis is on the internal network only **and** password-protected with
  `--requirepass` from a secret. Reachable-without-a-password is the standard path
  from "on the network" to remote code execution via `CONFIG SET` and `MODULE LOAD`,
  and "it's only on an internal network" stops being true the first time something
  else on that network has an SSRF.
- `--protected-mode yes` as a second line of defence.
- The object store is not routed publicly. Files reach clients through the
  application, which is where the authorization decision lives (ADR-19).
- Metrics and health are on a separate port (9000) that Traefik does not route.

## HTTP headers

Split between the reverse proxy and the workload (ADR-42):

- **Proxy** — HSTS, `nosniff`, `Referrer-Policy`, `X-Frame-Options: DENY`,
  COOP/CORP `same-origin`, `Permissions-Policy` denying every device capability.
- **Frontend Nginx** — the Content-Security-Policy, because `script-src` is a
  statement about who serves the scripts.

`script-src 'self'` with no `unsafe-inline` is what makes tokens in browser storage
an acceptable trade (ADR-27). Loosening it changes that calculation.

## Application

- Non-root UID 1001 in the image, fixed rather than assigned, because the secret
  files have to be readable by it.
- Every endpoint requires a role; the one exception is the token-authorized download,
  which validates its signed token as its first statement.
- Every read is owner-scoped in the query, not filtered afterwards. An ownership check
  in the caller is an ownership check somebody forgets.
- A 500 response never carries the exception message (ADR-08).
- A bad download token answers 404, not 401 — an authentication error would confirm
  that the id exists.

## Deliberately left open

**No `read_only: true` on the backend container.** The JVM writes to `/tmp` (uploads,
heap dumps) and the runtime layout writes under `/deployments`. A blanket read-only
mount trades a visible risk for subtle runtime failures in the native transport and
the dump path. `no-new-privileges` plus `cap_drop: ALL` carry most of the benefit.
Revisit with explicit `tmpfs` mounts if it becomes a requirement.

**No rate limiting at the proxy.** Traefik's rate limit is per source IP, which
throttles everyone behind one NAT collectively. The protections that exist instead
are the connection-pool acquisition timeout (a saturated pool fails fast rather than
queueing), the request body limit, and the caching of the hot read path. Add a rate
limit when there is a specific abuse case, and pick the key deliberately.

**No mutual TLS between containers.** Everything on `app-network` is on one host with
no external reachability. mTLS here would add certificate rotation to the operational
surface for a threat model that does not include an attacker already on the host's
docker network — at which point the certificates are readable too.

**No secret rotation automation.** Rotating the download-token key invalidates every
outstanding token, which is acceptable at a five-minute TTL. Rotating the database
password needs a coordinated restart. Both are manual and documented rather than
automated, because the automation would be used once a year and be broken when needed.

## Verifying

```bash
docker compose config                                  # the compose file parses
docker compose --profile monitoring config             # so does the optional profile
docker compose exec backend id                         # runs as 1001
docker compose exec backend cat /proc/1/status | grep Cap   # empty effective set
docker compose exec redis redis-cli ping               # NOAUTH — the password is required
curl -sI https://app.example.com | grep -i strict-transport   # the proxy middleware is loaded
```
