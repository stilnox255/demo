import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
    setStore,
    authenticatedFetch,
    tokensRefreshed,
    logout
} from './AuthControl.js';
import { setStore as setNotificationStore } from '../../notification/control/NotificationsControl.js';

function jwtWithExp(secondsFromNow) {
    const b64 = (obj) => btoa(JSON.stringify(obj))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    const payload = { exp: Math.floor(Date.now() / 1000) + secondsFromNow };
    return `header.${b64(payload)}.signature`;
}

describe('sessionExpired — repeat-suppression', () => {
    let toastDispatches;

    beforeEach(() => {
        vi.useFakeTimers();
        toastDispatches = [];
        // Authenticated probe always returns 401 → triggers refreshTokens().
        globalThis.fetch = vi.fn(async () => new Response('', { status: 401 }));
        globalThis.localStorage = {
            setItem: () => {},
            getItem: () => null,
            removeItem: () => {}
        };

        // refreshToken: null forces doRefresh() → false on the first hop, so
        // every authenticatedFetch reliably reaches the sessionExpired() branch.
        // dispatch is a no-op; we only care about the module-local sessionExpiredToastShown flag.
        setStore({
            getState: () => ({
                auth: {
                    accessToken: 'access',
                    refreshToken: null,
                    oidcConfig: { token_endpoint: 'https://kc/token' },
                    appConfig: { authConfig: { clientId: 'c' } }
                }
            }),
            dispatch: () => {}
        });

        setNotificationStore({
            dispatch: (action) => {
                if (action?.type === 'addToastAction') toastDispatches.push(action.payload);
            }
        });

        // Reset module-level sessionExpiredToastShown between tests by going
        // through the same path the production code uses.
        logout();
        toastDispatches = [];
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('shows the toast on the first refresh failure', async () => {
        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        expect(toastDispatches).toHaveLength(1);
        // Catalogue keys, not text: ToastContainer resolves them at render time so
        // an open toast follows a locale switch.
        expect(toastDispatches[0]).toMatchObject({
            type: 'error',
            title: 'auth.sessionExpired.title',
            detail: 'auth.sessionExpired.detail',
            action: { label: 'auth.sessionExpired.action', type: 'login' }
        });
    });

    it('does NOT stack additional toasts while the first is still visible', async () => {
        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        expect(toastDispatches).toHaveLength(1);
    });

    it('re-arms after a successful tokensRefreshed: a future failure dispatches again', async () => {
        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        expect(toastDispatches).toHaveLength(1);

        tokensRefreshed({
            accessToken: jwtWithExp(1800),
            refreshToken: 'r2',
            idToken: 'i2'
        });

        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        expect(toastDispatches).toHaveLength(2);
    });

    it('re-arms after logout: a future failure dispatches again', async () => {
        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        expect(toastDispatches).toHaveLength(1);

        logout();

        await expect(authenticatedFetch('https://example/api')).rejects.toThrow('Session expired');
        expect(toastDispatches).toHaveLength(2);
    });
});
