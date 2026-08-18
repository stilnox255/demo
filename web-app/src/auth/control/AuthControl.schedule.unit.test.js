import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { setStore, loginSuccess, logout } from './AuthControl.js';

function jwtWithExp(secondsFromNow) {
    const b64 = (obj) => btoa(JSON.stringify(obj))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    const payload = { exp: Math.floor(Date.now() / 1000) + secondsFromNow };
    return `header.${b64(payload)}.signature`;
}

describe('scheduleTokenRefresh', () => {
    let refreshCalls;

    beforeEach(() => {
        vi.useFakeTimers();
        refreshCalls = 0;
        globalThis.localStorage = { setItem: () => {}, getItem: () => null, removeItem: () => {} };
        globalThis.fetch = vi.fn(async () => {
            refreshCalls++;
            return new Response('{}', { status: 400 });
        });
        setStore({
            getState: () => ({ auth: {
                oidcConfig: { token_endpoint: 'https://kc/token' },
                appConfig: { authConfig: { clientId: 'c' } },
                refreshToken: 'r1'
            }}),
            dispatch: () => {}
        });
    });

    afterEach(() => { vi.useRealTimers(); });

    it('fires (exp - 120s) seconds after loginSuccess for a 1800s token', () => {
        loginSuccess({ accessToken: jwtWithExp(1800), refreshToken: 'r', idToken: 'i', userInfo: {} });
        vi.advanceTimersByTime(1679 * 1000);
        expect(refreshCalls).toBe(0);
        vi.advanceTimersByTime(2000);
        expect(refreshCalls).toBe(1);
    });

    it('clamps to 30s floor for a token that expires in 60s', () => {
        loginSuccess({ accessToken: jwtWithExp(60), refreshToken: 'r', idToken: 'i', userInfo: {} });
        vi.advanceTimersByTime(29 * 1000);
        expect(refreshCalls).toBe(0);
        vi.advanceTimersByTime(2000);
        expect(refreshCalls).toBe(1);
    });

    it('logout cancels the scheduled refresh', () => {
        loginSuccess({ accessToken: jwtWithExp(1800), refreshToken: 'r', idToken: 'i', userInfo: {} });
        logout();
        vi.advanceTimersByTime(2 * 60 * 60 * 1000);
        expect(refreshCalls).toBe(0);
    });

    it('re-arms the timer against the new exp after a successful refresh', async () => {
        loginSuccess({ accessToken: jwtWithExp(1800), refreshToken: 'r', idToken: 'i', userInfo: {} });

        // Replace the fetch mock so the first refresh SUCCEEDS, returning a new
        // token whose exp is 3600s ahead of the moment the timer fires.
        globalThis.fetch = vi.fn(async () => {
            return new Response(JSON.stringify({
                access_token: jwtWithExp(3600),
                refresh_token: 'r2',
                id_token: 'i2'
            }), { status: 200, headers: { 'content-type': 'application/json' } });
        });

        // Fire the first timer (1680s) → triggers refreshTokens() → tokensRefreshed
        // → scheduleTokenRefresh(new token). advanceTimersByTimeAsync flushes the
        // microtask queue between ticks so the awaited fetch + JSON parse complete.
        await vi.advanceTimersByTimeAsync(1680 * 1000);
        const callsAfterFirstRefresh = globalThis.fetch.mock.calls.length;
        expect(callsAfterFirstRefresh).toBe(1);

        // The new token's exp was minted at "now" (t=1680s) with +3600s ⇒ exp=5280s.
        // The new timer delay = (5280 - 1680 - 120) = 3480s. Stop 1s short of it.
        await vi.advanceTimersByTimeAsync(3479 * 1000);
        expect(globalThis.fetch.mock.calls.length).toBe(callsAfterFirstRefresh);

        // Cross the new boundary → second refresh fires.
        await vi.advanceTimersByTimeAsync(2 * 1000);
        expect(globalThis.fetch.mock.calls.length).toBe(callsAfterFirstRefresh + 1);
    });
});
