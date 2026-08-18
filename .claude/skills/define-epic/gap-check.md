# Concept Gap Check

Referenced by `define-epic` (Step 4) and `plan-epic` (Step 4). Walk the checklist and present findings to the user.

- **Goal coverage** — every motivation/user-story element reflected in scope?
- **Errors & edges** — failure modes, validation, concurrency considered?
- **Cross-cutting** — authn/authz, audit, i18n, telemetry, rate-limiting?
- **Data lifecycle** — CRUD + list for new entities; soft- vs. hard-delete?
- **Integration points** — S3, AI service, Keycloak, GitHub — all modeled?
- **Migration & compatibility** — schema/API changes have migration plan?
- **Non-functional** — perf budget, payload limits, concurrency expectations?
- **Testability** — every scope item verifiable?
- **UX completeness** — every new endpoint has a frontend surface (or explicit "backend-only")?
- **Out-of-scope honesty** — anything the user likely expects but isn't covered?

Present gaps as bullets. Ask the user: add to scope / defer to follow-up epic / accept as out-of-scope. Do not proceed until every flagged gap is resolved.
