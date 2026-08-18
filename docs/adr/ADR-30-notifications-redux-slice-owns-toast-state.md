# ADR-30: The Notifications Slice Owns Toast State

**Status:** Accepted
**Reversibility:** high — one slice.

## Context

Toast state could live in the container component. It is the only thing that
renders it, and component-local state is less machinery.

Then something outside the view tree needs to raise one — the token-refresh handler
discovering a dead session, an error path in a control module — and there is no way
in. The usual workaround is a module-level singleton with a callback, which is a
second state container with its own lifecycle, invisible to the store's devtools and
to anything that persists state.

## Decision

Toasts are a Redux slice like any other. `addToastAction` and `removeToastAction`,
a list of entries with an id, a type, a title and an optional detail.

Raising one is a dispatch, so any module can, including modules with no view.

The slice is deliberately **excluded from persistence**: a toast rehydrated from
storage would announce a session expiry from an hour ago on the next page load.
It is transient state that happens to live in a persisted store, and the
persistence layer skips it explicitly.

An entry may carry an **action** — a label plus a handler key. Handlers are
registered by name at startup (`registerToastActionHandler("login", login)`) rather
than stored in the slice: a function in Redux state is not serialisable, breaks
devtools, and cannot survive persistence. The name is data; the function stays in
the module that owns it.

## Rationale

One state container, one set of devtools, one place to look. The handler-by-name
indirection is the only non-obvious part, and it exists because the alternative
puts a function into serialisable state.

## Consequences

- The persistence layer names the slices it drops. That list is a thing to
  maintain, and the comment next to it says why each one is there.
- A toast action handler must be registered before a toast can use it — at startup,
  next to the auth initialization.

## Related

- ADR-29 — the component that renders the slice
- ADR-28 — dispatching from the control layer
