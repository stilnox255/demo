# deploy/

Everything the production stack needs, and nothing else (ADR-39). Copy this directory
to a host, generate the secrets, set the environment, and run
`docker compose up -d`.

## Prerequisites on the host

This stack deliberately does **not** run the pieces that are shared per host and
outlive any single application. They have to exist first:

| Prerequisite | Why it is not here |
|---|---|
| **Traefik**, in `network_mode: host`, with a `letsencrypt` cert resolver and a `redirect-https@file` middleware | One proxy per host serves every application. A second one would fight it for :443. |
| **Keycloak** with the realm from `keycloak-realm.json` in this directory | An identity provider is shared infrastructure with its own lifecycle and its own database, so this stack does not run it — but the realm it expects is versioned here. Adjust the domain and import it; there is no secret to copy back, because the backend holds no client credentials (ADR-45). See `DEPLOYMENT.md`. |
| **`monitoring_net`** — `docker network create monitoring_net` | An external network so whatever scrapes the exporters can join it and survive a `compose down`. |
| **`traefik-security-headers.yml`** copied into the proxy's dynamic-config directory | A middleware referenced by name that is not present makes every router fail to load. See the file's own header. |
| **A log collector** on `monitoring_net`, reachable under `LOG_COLLECTOR_ENDPOINT` (default `alloy:5140`) | One collector per host, like the proxy. The backend joins `monitoring_net`, so a collector living in another stack is reachable by name. Optional: syslog here is UDP and best-effort, so with nothing listening the JSON stream is simply dropped — the plain-text console log stays intact and `docker compose logs` is unaffected. |

Metrics and dashboards are optional here: with nothing to attach to, start the
`monitoring` profile (below) instead.

## First-time setup

```bash
# 1. Secrets. Idempotent — an existing file is never overwritten.
./setup-secrets.sh

# 2. Environment.
cp .env.example .env
$EDITOR .env          # DOMAIN, AUTH_DOMAIN, REGISTRY

# 3. Start.
docker compose pull
docker compose up -d
```

Secret files end up `0644` inside a `0700` directory. That is deliberate and
counter-intuitive: the container's UID is not the host owner, so `0600` makes the file
unreadable inside the container — and the failure surfaces as an authentication error
against Postgres or Redis, not as a permission error. Another user on the host
still cannot reach the files, because they cannot traverse the directory. The
reasoning is in `setup-secrets.sh` next to the mode.

## Deploying an update

From a repository clone, not from the host:

```bash
./deploy.sh root@example.com        # rolling: pull + up -d, health-gated
./deploy.sh -f root@example.com     # full: down, then pull + up -d
```

The script is restartable at every step: rsync converges, `compose up -d` recreates
only what changed, and it exits non-zero if the backend does not report healthy
(ADR-40). It never builds — images come from CI (ADR-38).

## Services

| Service | Purpose |
|---|---|
| `backend` | the application; :8080 public via Traefik, :9000 metrics and health, internal only |
| `frontend` | Nginx serving the built assets (ADR-25) |
| `postgres` | tuned for the container's own memory limit, not left at the defaults |
| `postgres-backup` | scheduled dumps into a volume |
| `redis` | cache, password-protected from a secret, `maxmemory` with `allkeys-lru` |
| `rustfs` | S3-compatible object storage |
| `rustfs-init` | creates the bucket and exits; idempotent, so a re-run is safe |
| `postgres-exporter`, `redis-exporter` | Prometheus metrics |
| `prometheus`, `grafana` | **profile `monitoring`** — off by default |

## Optional monitoring

```bash
docker compose --profile monitoring up -d
```

Off by default because a real host usually already runs one Prometheus for
everything, and the exporters here are attached to the external `monitoring_net` for
exactly that. Turn the profile on where there is nothing to attach to.

It brings `monitoring/prometheus.yml` (scraping the backend's *management* port,
not 8080), a provisioned Prometheus and Postgres datasource, one overview dashboard
and three alert rules. The dashboards are files in the repository rather than clicked
together in the UI: a dashboard that only exists in Grafana's database is lost with
the volume and cannot be reviewed in a diff.

## Backup and restore

```bash
./backup-db.sh                      # from cron or by hand
./backup-db.sh before-migration     # labelled
./backup-now.sh before-migration    # trigger it on the host over SSH, from a clone

./restore-db.sh                     # lists what is available
./restore-db.sh /srv/starter/backups/starter-….sql.gz
```

The scheduled container covers the routine case. These scripts are the operator path:
labelled, inspectable, and — the part that matters — a **restore**. A backup nobody
has restored is a hypothesis.

`restore-db.sh` takes a pre-restore snapshot first, unconditionally, and asks for the
database name as confirmation. "I restored the wrong backup" has to stay recoverable,
and the moment you need the script is the moment you are least likely to think of it
yourself.

Suggested cron on the host:

```cron
0 3 * * * /srv/starter/backup-db.sh >>/var/log/starter-backup.log 2>&1
```

## Files

| File | Role |
|---|---|
| `docker-compose.yml` | the stack, with per-service hardening (see SECURITY.md) |
| `.env.example` | everything the host must supply |
| `backend-entrypoint.sh` | secret files into environment variables; assembles the Redis URL |
| `rustfs-entrypoint.sh`, `grafana-entrypoint.sh` | the same job for those two |
| `healthcheck.sh` | container liveness, on the management port |
| `init-db.sh` | one-time role and database creation; Flyway owns the schema |
| `setup-secrets.sh` | generates missing secrets, never overwrites |
| `traefik-security-headers.yml` | installed into the host's proxy config (ADR-42) |
| `keycloak-realm.json` | imported into the host's identity provider (ADR-45) |
| `DEPLOYMENT.md` | the walkthrough, including a fresh host |
| `BUILD.md` | how the images are produced |
| `SECURITY.md` | the hardening checklist and what is deliberately left open |
