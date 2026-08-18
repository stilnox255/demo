import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { setStore } from './AuthControl.js';

// E29 T-07 — verify the module-level `storage` event listener registered
// at load in AuthControl.js. The listener is attached once on first
// import (jsdom provides `window`), so these tests share that single
// listener and verify the right Redux actions get dispatched when
// synthetic StorageEvents fire.

describe('cross-tab storage event listener', () => {
    let dispatched;
    let storage;

    beforeEach(() => {
        vi.useFakeTimers();
        dispatched = [];
        storage = {};
        // Replace localStorage with a controllable mock the listener reads.
        // `localStorage` is resolved at call time, so this override applies
        // to the listener body even though the listener was registered at
        // module load.
        globalThis.localStorage = {
            getItem: (k) => (k in storage ? storage[k] : null),
            setItem: (k, v) => { storage[k] = String(v); },
            removeItem: (k) => { delete storage[k]; }
        };
        setStore({
            getState: () => ({ auth: {} }),
            dispatch: (action) => { dispatched.push(action); }
        });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('dispatches tokensRefreshed when another tab writes access_token', () => {
        // jwt with exp far in the future so scheduleTokenRefresh does not
        // immediately fire and pollute the dispatch log.
        const newAccess = 'header.eyJleHAiOjk5OTk5OTk5OTl9.sig';
        storage.access_token = newAccess;
        storage.refresh_token = 'r-new';
        storage.id_token = 'i-new';

        window.dispatchEvent(new StorageEvent('storage', {
            key: 'access_token',
            newValue: newAccess
        }));

        const action = dispatched.find(a => a.type === 'tokensRefreshed');
        expect(action).toBeTruthy();
        expect(action.payload).toMatchObject({
            accessToken: newAccess,
            refreshToken: 'r-new',
            idToken: 'i-new'
        });
    });

    it('ignores storage events for non-token keys', () => {
        storage.access_token = 'should-not-be-read';

        window.dispatchEvent(new StorageEvent('storage', {
            key: 'theme',
            newValue: 'dark'
        }));

        expect(dispatched).toHaveLength(0);
    });

    it('dispatches logout when both access and refresh tokens are cleared', () => {
        // Both tokens absent → listener should treat this as a remote logout.
        window.dispatchEvent(new StorageEvent('storage', {
            key: 'access_token',
            newValue: null
        }));

        const action = dispatched.find(a => a.type === 'logout');
        expect(action).toBeTruthy();
    });

    it('reacts to refresh_token and id_token key changes too', () => {
        const newAccess = 'header.eyJleHAiOjk5OTk5OTk5OTl9.sig';
        storage.access_token = newAccess;
        storage.refresh_token = 'r-new';

        window.dispatchEvent(new StorageEvent('storage', {
            key: 'refresh_token',
            newValue: 'r-new'
        }));

        const action = dispatched.find(a => a.type === 'tokensRefreshed');
        expect(action).toBeTruthy();
        expect(action.payload.accessToken).toBe(newAccess);
    });
});
