# ADR-31: Error Toasts Are Automatic, Success Toasts Are Explicit

**Status:** Accepted
**Reversibility:** high — a placement rule for notification calls.

## Context

Two symmetrical mistakes are available.

**Every call site raises its own error toast.** The shared fetch wrapper already
surfaces failures, so the user gets two toasts for one failure. Worse, the call
sites that forget produce a silent failure — a button that does nothing, which is
the worst possible feedback.

**Every call site raises its own success toast.** The result is a notification for
every read, and users learn to dismiss toasts without reading them, which disarms
the mechanism for the case that matters.

## Decision

**Errors are automatic.** The authenticated fetch wrapper raises the toast for any
failed response, once, centrally. A control function does not raise an error toast;
it only records the failure in its own slice so the view can render a state.

**Success is explicit and rare.** A success toast is raised only where the user
performed an action whose effect they cannot otherwise see. Creating an item that
appears in the list needs no toast — the list is the feedback. Attaching a file
gets one, because the change is a small line in a table cell.

Inline error displays inside forms and views are not used. One mechanism for
failure feedback, in one place, so there is no question of which one a given failure
uses — and no half-migrated state where some views show errors twice and others not
at all.

## Rationale

The asymmetry follows from what the two mean. A failure is always worth reporting
and the user cannot be assumed to see it any other way. A success is usually
visible in the thing that changed, so announcing it adds noise.

Centralising the error path is also what makes it impossible to forget.

## Consequences

- A control function that needs a *specific* error message rather than the generic
  one raises it deliberately, and that is the exception.
- The fetch wrapper is the single place where an error message is derived from a
  problem-details body (ADR-08), so improving that derivation improves every error.

## Related

- ADR-29, ADR-30 — the mechanism
- ADR-08 — the error payload the message comes from
