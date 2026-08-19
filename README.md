# Starter

A reference implementation of a technology stack, and a starting point for new
projects.

There is no business logic here. What there is instead is one small aggregate — a
"demo item" — wired through every mechanism a real service needs, so that each
mechanism has a working example rather than a description: pagination, validation,
problem details, optimistic locking, file storage with signed downloads, a cached
projection with conditional GETs, a domain event, a bounded background job,
observability, and a deployment.

Every non-obvious decision is written down in [`docs/adr/`](docs/adr/README.md) with
its alternatives and its cost. That is the actual content of this repository; the
code is the demonstration.

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 25, Quarkus 3.38, Hibernate ORM with Panache, Flyway |
| API | JAX-RS, JSON-B, Bean Validation, OpenAPI, RFC 9457 problem details |
| Frontend | Web components, lit-html, Redux Toolkit, Vaadin Router, Vite |
| Storage | PostgreSQL 17, S3-compatible object storage |
| Cache | Redis, with an in-process backend in tests |
| Auth | OIDC via Keycloak, PKCE in the browser |
| Observability | Micrometer/Prometheus, OpenTelemetry, structured logs over syslog |
| Deployment | Docker Compose behind Traefik, images from a single registry |
| CI | GitHub Actions, path-filtered and sharded |
| Tests | JUnit 5, ArchUnit fitness functions, RestAssured, Vitest, Playwright, Gatling |

## Layout

```
src/main/java/de/ingoschindler/
├── kernel/            cross-cutting primitives — pagination, problem details,
│                      hashing, download tokens, uploads, object storage (ADR-02)
├── infrastructure/    exception mappers, request logging, MDC, health, clock
└── demo/              the one business component, in the hexagonal layout (ADR-01)
    ├── domain/                    pure records, no framework
    ├── application/port/{in,out}/ published API and outbound contracts
    ├── application/usecase/       one class per use case
    └── adapter/{in,out}/          REST, scheduled, messaging / persistence

web-app/               frontend, boundary/control/entity per feature (ADR-28)
load-test/             Gatling capacity tests (ADR-43)
deploy/                self-contained deployment unit (ADR-39)
docs/adr/              45 decision records
docs/guidelines/       empty extension point for project policy
.claude/               the hook that keeps test runs scoped
```

## Prerequisites

Nothing is vendored and nothing is auto-provisioned, so these have to be on the
machine first:

| | |
|---|---|
| **JDK 25** | the Gradle toolchain requests it and no resolver is configured, so an older JDK fails the build rather than downloading one |
| **Docker** | dev mode and the integration suite start real containers |
| **Node 22** | for `web-app/` |
| **`jq`** | only if you use Claude Code here — `.claude/hooks/block-unscoped-gradle-tests.sh` runs under `set -euo pipefail`, so without `jq` *every* shell call fails, not just the Gradle ones |

## Running it

One command. Dev services provision PostgreSQL, Keycloak and the S3 emulator, and
Quarkus also starts `compose-devservices.yml` — a reverse proxy that mirrors the
production path routing:

```bash
./gradlew quarkusDev
```

The API is on `http://localhost:8080`, the Dev UI on `/q/dev-ui`, OpenAPI on
`/openapi`. Users `admin/admin` and `user/user` exist in the dev realm.

Add the frontend for the full single-origin setup:

```bash
cd web-app && npm install && npm run dev    # Vite on 5173
# then browse http://localhost/ — same paths as production, HMR through the proxy
```

**If :80 is already taken on your machine**, set `DEV_PROXY_PORT` in `.env` before
starting. The compose start is all-or-nothing, so a failed port bind aborts it and dev
mode does not come up — and the actual cause is several stack traces down in the
output.

## Verifying it

```bash
./gradlew spotlessApply checkstyleMain checkstyleTest
./gradlew test --tests 'de.ingoschindler.*'                # unit + ArchUnit
./gradlew quarkusRuntimeTest --tests 'de.ingoschindler.*'   # integration, needs Docker
cd web-app && npm run test:unit && npm run build
```

Note the `--tests` filter. A hook in `.claude/` blocks unscoped `test`, `build` and
`check` invocations, because they pull in the full integration suite — see
`.claude/hooks/block-unscoped-gradle-tests.sh`.

## Using it as a starting point

Rename in this order:

1. **Package** — `de.ingoschindler` throughout `src/`. An IDE-wide rename is safer
   than a `sed`; the string also appears in `build.gradle` (`group`),
   `application.properties` (log categories, fault-tolerance keys) and the ArchUnit
   `BASE_PACKAGE` constant.
2. **Project name** — `starter` is by far the widest of these, spread over ~50 files.
   Do not work from a list; any list here goes stale the moment a file is added. Get
   the current one:

   ```bash
   grep -rIln 'starter\|STARTER_' \
       --exclude-dir={node_modules,dist,build,.git,.gradle,.idea} .
   ```

   Add `-i` for the product name as the user sees it: "Starter Admin" and the
   footer live in `web-app/src/i18n/entity/{en,de}.js` under `app.title` and
   `app.footer`, and nowhere else in the frontend (ADR-46).

   The obvious hits are the Gradle project name, the config prefix in
   `application.properties` and the image names. The ones people miss are in
   `deploy/`: the `/srv/starter` remote path, the `STARTER_*` variables, the
   Prometheus job name, the Grafana provisioning, and the Keycloak realm in
   `keycloak-realm.json`. One file *name* carries it too,
   `deploy/monitoring/grafana/dashboards/starter-overview.json`.
3. **Config prefix** — `starter.*` in `application.properties` and the
   `@ConfigProperty` names that read it.
4. **Registry** — `registry.ingoschindler.de` and the `ingoschindler/` image
   namespace. One input default in `.github/workflows/build-push-image.yml`, the
   `REGISTRY` defaults in `deploy/docker-compose.yml` and `cleanup-registry.sh`, and
   `quarkus.container-image.*` in `application.properties`. CI also expects
   `SELFHOSTED_REGISTRY_USER` / `SELFHOSTED_REGISTRY_TOKEN` as repository secrets.
5. **The demo component** — delete `de.ingoschindler.demo` and its tests, and add
   your own component in the same shape. The ArchUnit suite will tell you if the
   shape is wrong.

   One thing in it is a decision rather than a shape, and no fitness function will
   catch it: the read scope. Every port here takes an `ownerId`, which is the
   narrowest scope available and wrong for any product whose rows are shared by a
   team or a tenant. Settle that before the first query exists — ADR-47 has the
   alternatives and what each costs.
6. **`LICENSE`** — MIT with the original author's name in the copyright line.
7. **ADRs** — keep them. They describe the stack, not the demo. Adjust the ones
   whose trade-off differs for your project, and record *that* as the change.

What to keep untouched unless you have a reason: `kernel/`, `infrastructure/`,
`config/`, and the CI workflows.

## Guidelines

There are none, deliberately. What binds is in [`docs/adr/`](docs/adr/README.md), and
the per-area rules are in `src/CLAUDE.md` and `web-app/CLAUDE.md`.
[`docs/guidelines/`](docs/guidelines/) is an empty extension point: its `_index.md`
describes how to add a policy and wire it into `CLAUDE.md` if your project needs one.

## License

MIT — see [LICENSE](LICENSE).
