import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setStore, refreshTokens } from './AuthControl.js';

describe('refreshTokens — single-flight', () => {
    let fetchCalls;

    beforeEach(() => {
        fetchCalls = 0;
        globalThis.fetch = vi.fn(async () => {
            fetchCalls++;
            await new Promise(r => setTimeout(r, 10));
            return new Response(JSON.stringify({
                access_token: 'new', refresh_token: 'r2', id_token: 'i2'
            }), { status: 200, headers: { 'content-type': 'application/json' } });
        });
        globalThis.localStorage = { setItem: () => {}, getItem: () => null, removeItem: () => {} };
        setStore({
            getState: () => ({ auth: {
                oidcConfig: { token_endpoint: 'https://kc/token' },
                appConfig: { authConfig: { clientId: 'c' } },
                refreshToken: 'r1'
            }}),
            dispatch: () => {}
        });
    });

    it('issues one POST for two concurrent callers', async () => {
        const [a, b] = await Promise.all([refreshTokens(), refreshTokens()]);
        expect(a).toBe(true);
        expect(b).toBe(true);
        expect(fetchCalls).toBe(1);
    });

    it('a fresh call after settlement issues a new POST', async () => {
        await refreshTokens();
        await refreshTokens();
        expect(fetchCalls).toBe(2);
    });

    it('propagates failure to all concurrent waiters and clears the slot', async () => {
        globalThis.fetch = vi.fn(async () => {
            fetchCalls++;
            return new Response('{}', { status: 400 });
        });
        const [a, b] = await Promise.all([refreshTokens(), refreshTokens()]);
        expect(a).toBe(false);
        expect(b).toBe(false);
        expect(fetchCalls).toBe(1);
        await refreshTokens();
        expect(fetchCalls).toBe(2);
    });
});
