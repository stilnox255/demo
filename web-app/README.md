# Starter Frontend

Single-page application on web components, lit-html, Redux Toolkit and Vaadin
Router, built with Vite. Served in production by its own Nginx container behind the
reverse proxy at the same host as the backend — see
[ADR-25](../docs/adr/ADR-25-frontend-served-by-dedicated-nginx-container.md).

No framework runtime beyond those three libraries. Custom elements, `fetch`,
`URLSearchParams`, `<dialog>` and CSS nesting cover most of what a framework would
be pulled in for.

## Development

```bash
npm install
npm run dev
```

Two ways to run it:

- **Direct Vite** — `http://localhost:5173`. Vite's own proxy forwards `/api`, `/q`
  and `/.well-known` to the backend on 8080 (see `vite.config.js`).
- **Through the dev proxy** (recommended) — start
  `docker compose -f compose-devservices.yml up -d` in the repository root and open
  `http://localhost`. Same paths and same origin as production, and HMR works through
  the proxy. One origin locally is worth the extra container: two origins mean CORS
  and cookie behaviour that exists nowhere else.

## Production output

```bash
npm run build
```

Vite emits hashed assets to `dist/`, which the CI pipeline packages into the Nginx
image on merge (ADR-38 — the image never rebuilds what was already built and tested).

## Structure

```
web-app/
├── src/
│   ├── index.html                 entry; Vite bundles the module graph
│   ├── app.js                     startup: theme, then the OIDC sequence (ADR-26)
│   ├── store.js                   one Redux store, one reducer per feature
│   ├── BElement.js                base custom element: subscribe, extract, render
│   ├── style.css                  one stylesheet, custom properties for theming
│   ├── auth/ theme/ notification/ localstorage/ health/ info/ landing/
│   ├── navigation/                app shell and routing
│   ├── shared/                    data table and pagination controls
│   └── demo/                      the demo feature, boundary/control/entity
├── public/                        copied verbatim to the dist root
├── tests/                         Playwright specs and auth setup
├── nginx.conf.template            rendered at container start (ADR-42)
├── Dockerfile
├── vite.config.js / vitest.config.js / playwright.config.js
└── package.json
```

## Per-feature layout

Three directories per feature, and the boundaries are strict (ADR-28):

- **boundary** — components. Read the store, render, call control functions. No
  `fetch`, no `dispatch`.
- **control** — side effects. Every network call and every dispatch.
- **entity** — the reducer. A pure function of `(state, action)`.

## Tests

```bash
npm run test:unit    # vitest + jsdom, files named *.unit.test.js next to the source
npm test             # Playwright; boots the full dev stack including Keycloak
```

The Playwright suite is a deliberate manual step rather than a CI job (ADR-32): it
needs the identity provider running.
