# Deployment

## Routine update

Once CI has pushed images for the latest commit on the default branch, from a
repository clone:

```bash
./deploy.sh root@example.com        # rolling update
./deploy.sh -f root@example.com     # full restart (down first)
```

The script rsyncs `deploy/` to `/srv/starter` on the host and runs
`docker compose pull && up -d` over SSH, then waits for the backend to report healthy
and exits non-zero if it does not (ADR-40). It never builds — the images already
exist, and a deploy that builds ships an artifact nothing tested.

The rolling path recreates only containers whose image or configuration changed, so a
frontend-only deploy leaves the database and the backend running.

Use `-f` when a convergent restart is not enough: a changed network definition, or a
container in a wedged state.

## Fresh host, once

### 1. Host prerequisites

This stack does not run the shared pieces (ADR-39). They come first:

```bash
# The external network whatever scrapes the exporters will join.
docker network create monitoring_net
```

Plus, from your infrastructure setup:

- **Traefik** in `network_mode: host`, with a `letsencrypt` cert resolver and a
  `redirect-https@file` middleware.
- **Keycloak**, reachable at `AUTH_DOMAIN`, with the realm from
  `keycloak-realm.json` in this directory. It is the realm the compose file already
  references by name. Edit the two `app.example.com` occurrences to your domain, then
  import it through *Create realm -> Browse* in the admin console, or:

  ```bash
  kcadm.sh config credentials --server https://auth.example.com \
      --realm master --user admin
  kcadm.sh create realms -f keycloak-realm.json
  ```

  It ships **no users**, and there is no secret to copy afterwards: the backend
  validates tokens against this realm's public keys and holds no credentials of its
  own (ADR-45). One step is left, in *Users*: create the first account and grant it
  the `admin` realm role.

  This is not the dev realm. `src/main/resources/realm_configuration-dev.json` has
  two throwaway logins, `sslRequired: none` and no password policy at all, because it
  is thrown away on every restart. This one carries the settings from a Keycloak realm
  that is actually in production: a password policy, brute-force lockout with a
  backoff, TLS for anything not on a private address, and a five-minute access token.
  Self-registration is the one place it deviates on purpose. Turn it back on in
  *Realm settings -> Login* if your product wants it.

  Everything Keycloak creates by itself on import is deliberately not in the file:
  the built-in clients, the authentication flows, the default client scopes and the
  key providers. A realm export contains all of it, several hundred lines of it, and
  none of it survives review because nobody can tell which parts were changed.
- **`traefik-security-headers.yml`** from this directory, copied into Traefik's
  dynamic-config directory (typically `/srv/proxy/dynamic/`). Traefik reloads dynamic
  files by itself. A middleware referenced by name that is not present makes **every**
  router fail to load, so this is not optional.

DNS: `app.${DOMAIN}` and `auth.${DOMAIN}` pointing at the host.

### 2. Get the files there

```bash
ssh root@example.com 'mkdir -p /srv/starter'
./deploy.sh root@example.com    # will fail at the end; the stack is not configured yet
```

### 3. Secrets, on the host

```bash
cd /srv/starter
./setup-secrets.sh
./setup-secrets.sh show    # store these in a password manager
```

Every secret the stack needs is generated here. Nothing has to be fetched from
Keycloak.

Idempotent: an existing secret is never overwritten, so re-running after adding a new
one does the right thing and cannot silently rotate a password other components hold.

### 4. Environment, on the host

```bash
cp .env.example .env
vi .env      # DOMAIN, AUTH_DOMAIN, KEYCLOAK_REALM, REGISTRY, image tags
```

### 5. Start

```bash
docker compose pull
docker compose up -d
docker compose ps
```

`init-db.sh` runs once, on the empty data directory, and creates the application role
and its database. Flyway then owns the schema from inside the application — this
script deliberately creates no tables, because two things creating schema is one thing
too many.

### 6. Verify

```bash
docker compose ps                          # everything healthy
curl -sf https://app.example.com/q/health/ready
curl -sf https://app.example.com/.well-known/app-config
docker compose logs -f backend
```

Then log in through the frontend at `https://app.example.com` and create an item. The
smoke test that covers the most wiring in one action: it exercises auth, the database,
validation and the cache invalidation.

### 7. Backups

```cron
0 3 * * * /srv/starter/backup-db.sh >>/var/log/starter-backup.log 2>&1
```

Then **restore one**, into a scratch database, before you need to. A backup nobody has
restored is a hypothesis. `restore-db.sh` takes a pre-restore snapshot first and asks
for the database name as confirmation.

## Optional monitoring

```bash
docker compose --profile monitoring up -d
```

Grafana appears at `grafana.${DOMAIN}` with the admin password from
`secrets/grafana_admin_password.txt`. Leave the profile off where the host already runs
a Prometheus — the exporters are on `monitoring_net` for that (ADR-39).

## Rolling back

Deploying an earlier image tag:

```bash
ssh root@example.com
cd /srv/starter
vi .env                        # BACKEND_IMAGE_TAG=jvm-<earlier-sha>
docker compose pull && docker compose up -d
```

There is no rollback command, on purpose: the tag is already a parameter, and that is a
smaller mechanism than a feature that has to be maintained and tested.

**Migrations are the exception.** Flyway rolls forward, never back. A schema change
that is not backward compatible with the previous application version cannot be rolled
back by changing a tag — so migrations are written to be compatible with the running
version (add a nullable column, backfill, then start using it), and that constraint is
a review item on every migration.

## Troubleshooting

**Backend restarting.** `docker compose logs backend`. The most common cause is an
unreadable secret file, and the entrypoint says so explicitly by name rather than
letting it surface later as an authentication failure.

**Login fails, everything healthy.** `AUTH_DOMAIN` or `KEYCLOAK_REALM` is wrong, so
the backend cannot reach the realm's JWKS or the issuer in the token does not match
the one it was configured with. It logs one or the other. A realm imported without the
audience mapper looks similar from the browser but fails at authorization, not
validation.

**Login redirects and comes back logged out.** The frontend's CSP is blocking the
identity provider. Check `AUTH_ORIGIN` on the frontend container against the browser
console.

**502 from Traefik.** The router's `traefik.docker.network` label and the network the
container is actually on disagree, or the middleware file is not installed.

**Slow requests, database looks idle.** The connection pool. `agroal_awaiting_count`
on the dashboard; a non-zero value there means requests are queueing for a connection
and will fail after five seconds by design (ADR-21 explains why that is the intended
behaviour rather than a longer wait).
