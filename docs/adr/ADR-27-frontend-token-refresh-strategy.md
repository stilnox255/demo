# ADR-27: Frontend Token-Refresh Strategy

**Status:** Accepted
**Reversibility:** medium — it is the session behaviour users feel.

## Context

An access token expires. What happens then is the difference between an
application people keep open all day and one that logs them out mid-form.

Each single mechanism has a hole:

- **Refresh on a timer only.** A laptop that was asleep wakes up with an expired
  token and a timer that fired into nothing.
- **Refresh on 401 only.** The user's action is what discovers the expiry, so the
  first request after every idle period is slow — and a page that fires five
  parallel requests fires five parallel refreshes, four of which race.
- **Ignore other tabs.** Two tabs each hold their own refresh token. One refreshes,
  the provider rotates the token, and the other tab's token is now invalid. The
  user is logged out of a tab they were using.

## Decision

All four mechanisms, because each covers a different hole:

1. **Proactive.** A timer refreshes shortly before expiry, so the common case never
   costs the user a slow request.
2. **Reactive.** A 401 from any API call triggers a refresh and one retry of that
   call. This is the safety net for the case the timer missed — a suspended
   machine, a clock skew, a token revoked server-side.
3. **Single-flight.** Concurrent refresh attempts share one in-flight promise.
   Without it, a page load that fires several requests fires several refreshes, and
   with token rotation all but one of them get an invalidated token.
4. **Cross-tab.** The refreshed tokens are written to shared storage and other tabs
   pick them up. One tab refreshes, every tab benefits, and no tab is logged out by
   another tab's rotation.

When a refresh genuinely fails, the session ends deliberately: state is cleared and
a toast with a login action is shown, rather than a redirect that would discard
whatever the user was doing.

## Rationale

The combination is more code than any single mechanism, and each part exists
because of a specific failure the others do not cover. The single-flight guard is
the one that looks optional and is not: refresh-token rotation turns a benign race
into a logout.

## Consequences

- The refresh logic is the most-tested part of the frontend, with unit coverage per
  mechanism — scheduling, storage, session end, error paths. It is worth it: this is
  the code that decides whether the app feels reliable.
- Tokens live in shared browser storage, which is a deliberate XSS trade-off. The
  CSP (ADR-42) is the mitigation, and it is why `script-src 'self'` is not
  negotiable.

## Related

- ADR-26 — the bootstrap this continues
- ADR-31 — how the failure is surfaced
