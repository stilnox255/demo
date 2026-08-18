# ADR-39: deploy/ Is a Self-Contained Deployment Unit

**Status:** Accepted
**Reversibility:** medium — the directory layout is what the deploy script syncs.

## Context

Deployment files tend to scatter: a compose file at the repository root, an
entrypoint next to the source, a secrets script in a scripts directory, an
environment example somewhere else. Deploying then means knowing which files matter,
and the answer lives in someone's head.

The related failure is a compose file that references paths outside its own
directory. It works from the repository root and breaks on the host, where only some
of the tree exists.

## Decision

Everything the deployment needs lives in `deploy/`, and every path inside it is
relative to itself. Copying that one directory to a host and running
`docker compose up -d` there is the whole deployment.

```
deploy/
├── docker-compose.yml          the stack
├── .env.example                what the host must supply
├── backend-entrypoint.sh       secret files -> environment
├── rustfs-entrypoint.sh
├── grafana-entrypoint.sh
├── healthcheck.sh              container liveness probe
├── init-db.sh                  one-time role and database creation
├── setup-secrets.sh            generates secrets on the host, idempotent
├── backup-db.sh / restore-db.sh / backup-now.sh
├── traefik-security-headers.yml   installed into the host's proxy config
├── keycloak-realm.json            imported into the host's identity provider
├── monitoring/                 optional profile: scrape config, dashboards, alerts
├── README.md / DEPLOYMENT.md / BUILD.md / SECURITY.md
└── secrets/                    generated on the host, never committed
```

What the unit deliberately does **not** contain, because it is shared per host and
outlives any single application: the reverse proxy, the identity provider, and
(unless the optional profile is used) the metrics stack. Those are prerequisites,
and they are named as prerequisites in the documentation rather than assumed.

The *configuration* those prerequisites need to serve this application is a different
thing, and it does live here: `traefik-security-headers.yml` and
`keycloak-realm.json`. Both get installed into a component this stack does not run,
and both are versioned with the application, because that is what they follow. A
router name and a client id change with the code, not with the host.

The deploy script syncs the directory with `--delete`, excluding `.env` and
`secrets/` — those are generated on the host and only exist there, so syncing would
delete them.

## Rationale

"Copy one directory, run one command" is a deployment anyone can perform and
anyone can review. The alternative is a runbook, and a runbook drifts.

Keeping the shared infrastructure out is the same decision from the other side: a
compose file that also started a reverse proxy would fight with the one already
running on the host.

## Consequences

- Some duplication between the development compose file and this one. They are
  different environments with different concerns, and merging them produces a file
  with conditionals in it.
- A deployment prerequisite list exists and has to be kept honest.

## Related

- ADR-40 — how the directory gets there
- ADR-24 — how the values reach the containers
