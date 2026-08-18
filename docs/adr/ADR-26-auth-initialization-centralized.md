# ADR-26: Auth Initialization Centralized in the Entry Module

**Status:** Accepted
**Reversibility:** high — one module.

## Context

OIDC bootstrap has an order: fetch the public application configuration, discover
the provider's endpoints from its well-known document, then check for an existing
session. Every step depends on the previous one having finished.

Left to the components that need authentication, each one guards with "have we
loaded the config yet?" and triggers the load if not. Several components mounting
at once then trigger several parallel discovery requests, the order becomes
whatever the render order happens to be, and a component can render against a
half-initialized auth state.

## Decision

One place — the application entry module — runs the sequence, once, before
anything else needs it:

```javascript
async function initApp() {
    const appConfig = await loadAppConfig();
    await loadOidcConfig(appConfig.authConfig.issuer);
    await checkAuth();
}
initApp();
```

No component initializes authentication. Components read the auth state from the
store and render a loading state while it is not ready — which they have to do
anyway, since the first paint happens before the network round trips finish.

The theme is initialized in the same place and for the same reason: it has to be
applied before first paint to avoid a flash of the wrong theme, and that is an
ordering constraint, not a component concern.

## Rationale

The sequence has a fixed order and runs exactly once. That makes it startup logic,
and startup logic belongs at the entry point where the order is visible in five
lines.

Distributing it also distributes the failure handling: with one call site there is
one place where "the identity provider is unreachable" is handled.

## Consequences

- A component that needs auth state renders a loading branch. That is honest —
  there genuinely is a moment before the state exists.
- The store is the only channel between the auth module and the components, which
  is what keeps them from calling into it.

## Related

- ADR-27 — keeping the session alive after this bootstrap
- ADR-28 — why the store is the only channel
