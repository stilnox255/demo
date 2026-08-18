# Frontend — Project-specific Rules

New views must match the look and feel of the existing ones — see
[`DESIGN.md`](DESIGN.md) for the tokens and reusable components to build them from.

## Commands

```bash
npm install
npm run dev        # Vite on http://localhost:5173
npm run test:unit  # vitest
npm run build      # hashed assets into dist/
```

## Layering (ADR-28)

Per feature: `boundary/` components, `control/` side effects, `entity/` reducer.

- A component never calls `fetch` and never dispatches. It reads state and calls a
  control function.
- A reducer never fetches. It is a pure function of `(state, action)`.
- The store is handed to each control module through a `setStore` setter, not
  imported — the reducers import their action creators from the control module, so
  importing the store back would close the cycle.

## Notifications (ADR-29, ADR-30, ADR-31)

- Error toasts are raised **centrally** by the authenticated fetch wrapper. Do not
  raise one per call site; the user would get two for one failure.
- Success toasts are **explicit and rare** — only where the effect is not visible in
  what changed on screen.
- No inline error displays in forms or views. One mechanism, one place.
- Raising a toast is a dispatch, so any module can, including one with no view.

## Deployment

Own Nginx container, not a bundle inside the backend (ADR-25). Assets that must live
at the site root (`favicon.ico`, `apple-touch-icon.png`, `callback.html`) go in
`public/`, which Vite copies verbatim.

`nginx.conf.template`, not a static `.conf`: the CSP's allowed auth origin differs
per deployment and is substituted at container start (ADR-42). Every `location` block
that adds any header re-adds the whole security set — Nginx replaces rather than
merges inherited headers, so omitting them silently unsets them for that location.

## Auth troubleshooting

For a report of an unexpected logout, walk the chain in
`src/auth/control/AuthControl.js` (ADR-27):

1. `checkAuth` — runs on every page load; attempts a refresh on 401 before giving up.
2. `refreshTokens` — single-flight: concurrent callers share one request. Without
   this, token rotation invalidates all but one of them.
3. `scheduleTokenRefresh` — fires shortly before expiry; logout cancels it.
4. The `storage` event listener — keeps tokens in step across tabs, so one tab's
   rotation does not log another out.
5. `sessionExpired` — a hard refresh failure shows a toast with a login action rather
   than redirecting, so the user does not lose what they were doing.
