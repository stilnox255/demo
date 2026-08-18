# CLAUDE.md

Reference implementation of a Java 25 / Quarkus 3.38 stack with a web-component
frontend. There is no business domain — one demo aggregate exercises every
mechanism, and the decisions behind them live in `docs/adr/`.

## Engineering Guidelines

Binding always-on policies live in `docs/guidelines/`. Follow them for all code
generation, edits, and reviews.

@docs/guidelines/_index.md

## Backend Architecture

Hexagonal Architecture (Ports & Adapters), one class per use case. See
`docs/adr/ADR-01-hexagonal-architecture.md` for the layering rules and the per-BC
package structure (`domain` / `application` / `adapter.in` / `adapter.out`).

Do not introduce `boundary` / `control` / `entity` packages in the backend — that is
a different layout and the ArchUnit suite rejects it. (The *frontend* does use
boundary/control/entity; that is a separate convention, see `ADR-28`.)

Architectural rules are enforced by
`src/test/java/de/ingoschindler/architecture/HexagonalArchitectureTest.java` — a
violation fails the build, not just review.

## Development Commands

```bash
./gradlew quarkusDev    # dev mode; starts PostgreSQL + Keycloak as dev services and
                        # compose-devservices.yml (object store + dev proxy) with them

# Tests are always scoped. A hook blocks unscoped build/check/test invocations
# because they drag in the full integration suite.
./gradlew test --tests 'de.ingoschindler.demo.*' --tests 'de.ingoschindler.architecture.*'
./gradlew quarkusRuntimeTest --tests 'de.ingoschindler.demo.*'
./gradlew spotlessApply checkstyleMain checkstyleTest

# Frontend
cd web-app && npm run dev          # Vite on 5173
cd web-app && npm run test:unit    # vitest
cd web-app && npm run build

# Capacity tests, against a deployed environment only (ADR-43)
./gradlew :load-test:runSimulation -Dgatling.simulationClass=simulations.DemoItemsSimulation
```

`quarkusDev` plus `npm run dev` in `web-app/` gives the full single-origin setup at
`http://localhost/`. Set `DEV_PROXY_PORT` in `.env` if :80 is taken.

## Frontend

Source in `web-app/`, shipped as a standalone Nginx container — **not** bundled into
the backend image (ADR-25). `npm run build` produces hashed assets in `web-app/dist/`,
which `web-app/Dockerfile` packages. The reverse proxy routes `/api`, `/q` and
`/.well-known` to the backend and everything else to Nginx at the same host.

There is no static-resource directory in the backend and no catch-all JAX-RS route.
Adding one reintroduces the problem ADR-25 removed.

## Infrastructure (dev)

Provided by Quarkus Dev Services in `quarkusDev`:

- PostgreSQL — Flyway migrations in `src/main/resources/db/migration/`
- Keycloak (8180) — realm from `src/main/resources/realm_configuration-dev.json`,
  users `admin/admin` and `user/user`
- S3 emulator — a native-boot image, chosen for spin-up cost

Also started by `quarkusDev`, from `compose-devservices.yml`: the persistent object
store and the dev reverse proxy. Quarkus discovers that file itself, which is what
makes dev mode a single command.

The failure mode to know: that compose start is all-or-nothing. The dev proxy publishes
:80, and if the port is taken the bind fails, the whole compose start aborts, and dev
mode never boots — the real cause sitting several stack traces down. `DEV_PROXY_PORT`
in `.env` moves it.

The cache runs on Redis in deployed environments and on the in-process backend in
tests, switched by `quarkus.cache.type` (ADR-41). No Redis container is needed to run
the suite.

## Project Rules

- Do not create or change files when opening the project — wait for instructions
- Ask before changing `build.gradle` or adding a dependency
- Keep designs KISS/YAGNI — ask before adding optional features or extension points
- Every non-obvious decision gets an ADR, with its alternatives and its cost

## Git Commands

- Never use `git -C <path>` — always run git commands from the current working
  directory
- Never start commands with a new line

## Bash Commands

- Avoid `$()` command substitution. Use pipes or store intermediate results via
  separate commands instead.
