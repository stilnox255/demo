# ADR-29: Global Toast Container as a Singleton Component

**Status:** Accepted
**Reversibility:** high — one component and its mount point.

## Context

Notifications have to appear regardless of which view is mounted. Per-view toast
containers produce the obvious problems: a toast raised as a view unmounts has
nowhere to render, two views mounted at once render two stacks, and each container
brings its own positioning.

The subtler problem is who can raise one. If a toast needs a reference to a
container, then only code holding that reference can notify — which excludes exactly
the places that most need to, like a session-expiry handler in the auth module.

## Decision

One `<toast-container>` element, mounted once in the application shell, above the
router outlet. It renders whatever is in the notifications slice.

Nothing holds a reference to it. Raising a notification is dispatching an action
(ADR-30), so any module can do it — including one with no view at all.

The container owns only presentation: stacking order, dismissal, and the timeout
that removes a toast. That timeout lives here rather than in the reducer, because a
reducer that schedules a `setTimeout` is no longer a pure function.

Mounted above the outlet, so a route change cannot unmount it mid-notification.

## Rationale

The alternative — passing a notifier down to whatever might need one — is
dependency injection by hand, and it stops at the first module that is not part of
the view tree.

## Consequences

- The shell always renders the container, even with nothing to show. It is an empty
  element; the cost is nil.
- A toast survives a route change, which is what makes "your session expired" work
  at all.

## Related

- ADR-30 — where the state lives
- ADR-31 — which events raise one
