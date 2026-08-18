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
docs/adr/              43 decision records
docs/guidelines/       binding engineering policies
.claude/               agent skills and hooks
```

## Running it

One command. Dev services provision PostgreSQL and Keycloak, and Quarkus also starts
`compose-devservices.yml` — the persistent object store and a reverse proxy that
mirrors the production path routing:

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
2. **Project name** — `starter` in `settings.gradle`, the container image names in
   `application.properties` and `deploy/`, the Keycloak realm, the database name.
3. **Config prefix** — `starter.*` in `application.properties` and the
   `@ConfigProperty` names that read it.
4. **The demo component** — delete `de.ingoschindler.demo` and its tests, and add
   your own component in the same shape. The ArchUnit suite will tell you if the
   shape is wrong.
5. **ADRs** — keep them. They describe the stack, not the demo. Adjust the ones
   whose trade-off differs for your project, and record *that* as the change.

What to keep untouched unless you have a reason: `kernel/`, `infrastructure/`,
`config/`, the CI workflows, and `docs/guidelines/`.

## Guidelines

Binding, always-on policy lives in [`docs/guidelines/`](docs/guidelines/), imported
from `CLAUDE.md`. Policy that only applies in specific contexts belongs in a skill
under `.claude/skills/` instead — see `docs/guidelines/_index.md` for both.

## License

MIT — see [LICENSE](LICENSE).
