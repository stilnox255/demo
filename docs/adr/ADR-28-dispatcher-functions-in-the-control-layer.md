# ADR-28: All Dispatcher Functions Belong in the Control Layer

**Status:** Accepted
**Reversibility:** high — it is a placement rule.

## Context

A web component that needs data can fetch it and dispatch the result itself. It is
the shortest path, and it puts a network call and a store shape into a file whose
job is markup.

What follows is predictable. The next component that needs the same data
reimplements the call, slightly differently. A change to the store shape means
finding every component that dispatches. And a component cannot be rendered in a
test without stubbing `fetch`.

## Decision

Three layers per feature, and the boundaries are strict:

- **boundary** — web components. Read state from the store, render, and call
  control functions. No `fetch`, no `dispatch`.
- **control** — side effects. Every network call and every dispatch lives here.
  Exports plain async functions the boundary calls.
- **entity** — the reducer. A pure function of `(state, action)`. Never fetches.

```javascript
// control
export const loadDemoItems = async (page = 1, pageSize = 25) => {
    storeInstance.dispatch(demoItemsLoadingAction());
    const response = await authenticatedFetch(`/api/demo-items?page=${page}&pageSize=${pageSize}`);
    ...
};

// boundary
connectedCallback() { setTimeout(() => loadDemoItems(), 0); }
```

The store is handed to each control module via a setter rather than imported. The
reducers import their action creators from the control module, so importing the
store back would close the cycle.

## Rationale

The boundary between "renders" and "causes effects" is the one that makes either
half testable: the reducer with plain values, the control module with a stubbed
fetch, the component with a pre-populated store.

It also gives every feature the same shape, so finding the network call for a
feature is a matter of looking in `control/`.

## Consequences

- A one-line fetch in a component becomes a two-line control function plus a call.
  That is the cost, and it is paid once per feature.
- Error toasts are raised in one place rather than per call site: the authenticated
  fetch wrapper already surfaces a failed response, so a toast in every control
  function would double up (ADR-31).

## Related

- ADR-31 — error and success notifications
- ADR-30 — the slice that holds toast state
