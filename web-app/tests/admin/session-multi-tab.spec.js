import { test, expect } from '@playwright/test';

// E29 T-07 — cross-tab token sync via storage events.
//
// When tab A writes new tokens to localStorage (e.g. after a successful
// refresh) or clears them (logout), tab B's AuthControl picks up the
// storage event and dispatches tokensRefreshed / logout so tab B's Redux
// store sees the new state without making its own token-endpoint call.
//
// Playwright cannot fire a real cross-tab storage event from one page into
// another, so tab A writes the value and then manually dispatches a
// synthetic StorageEvent on its own window — which the test then asserts
// would be received by tab B in production (we observe tab B's
// localStorage / DOM state). The unit test in
// AuthControl.storage.unit.test.js is the canonical proof of the listener
// wiring; this spec is the end-to-end smoke.

test.describe('Cross-tab token sync', () => {

    test('tokens refreshed in one tab are picked up by another', async ({ browser }) => {
        const ctx = await browser.newContext({ storageState: 'tests/.auth/admin.json' });
        const tabA = await ctx.newPage();
        const tabB = await ctx.newPage();
        await tabA.goto('/');
        await tabB.goto('/');
        await tabA.waitForSelector('nav.sidebar', { timeout: 20_000 });
        await tabB.waitForSelector('nav.sidebar', { timeout: 20_000 });

        const sentinel = 'sentinel-jwt.eyJleHAiOjk5OTk5OTk5OTl9.sig';

        // Tab A writes the new token to localStorage (shared with Tab B in
        // the same browser context) and synthesizes the cross-tab storage
        // event that Tab B would normally receive from the browser.
        await tabA.evaluate((token) => {
            localStorage.setItem('access_token', token);
            window.dispatchEvent(new StorageEvent('storage', {
                key: 'access_token',
                newValue: token
            }));
        }, sentinel);

        // Tab B: synthesize the same event (in real browsers it would arrive
        // cross-tab automatically; in a single Playwright context it must be
        // dispatched manually).
        await tabB.evaluate((token) => {
            window.dispatchEvent(new StorageEvent('storage', {
                key: 'access_token',
                newValue: token
            }));
        }, sentinel);

        // Tab B's Redux store must have absorbed the new token via the
        // storage-event listener — this is the assertion that fails if the
        // listener wiring is removed.
        const observedInStore = await tabB.waitForFunction(() => {
            const accessToken = globalThis.__appStore?.getState?.()?.auth?.accessToken;
            return accessToken && accessToken.startsWith('sentinel-jwt') ? accessToken : null;
        }, null, { timeout: 5_000 }).then(handle => handle.jsonValue()).catch(() => null);

        expect(observedInStore).toBeTruthy();
        expect(observedInStore.startsWith('sentinel-jwt')).toBe(true);

        await ctx.close();
    });

    test('logout in one tab logs out the other', async ({ browser }) => {
        const ctx = await browser.newContext({ storageState: 'tests/.auth/admin.json' });
        const tabA = await ctx.newPage();
        const tabB = await ctx.newPage();
        await tabA.goto('/');
        await tabB.goto('/');
        await tabA.waitForSelector('nav.sidebar', { timeout: 20_000 });
        await tabB.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // Tab A: clear the tokens (simulating logout) and dispatch the
        // storage event tab B would receive.
        await tabA.evaluate(() => {
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');
            window.dispatchEvent(new StorageEvent('storage', {
                key: 'access_token',
                newValue: null,
                storageArea: localStorage
            }));
        });

        // Tab B observes the same event (Playwright cannot fire cross-tab,
        // so re-emit). The listener should call logout() and the sidebar
        // should disappear within 2 s.
        await tabB.evaluate(() => {
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');
            window.dispatchEvent(new StorageEvent('storage', {
                key: 'access_token',
                newValue: null,
                storageArea: localStorage
            }));
        });

        await expect(tabB.locator('nav.sidebar')).toBeHidden({ timeout: 2_000 });

        await ctx.close();
    });
});
