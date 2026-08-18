import { test, expect } from '@playwright/test';

// Covers the toast action mechanism itself rather than a feature that uses it.
// Driven through the __appTestHooks debug hook installed by
// NotificationsControl.js, because the real trigger — a failed token refresh — is
// hard to provoke on demand and this asserts the notification, not the auth path.

test.describe('Toast action button', () => {

    test('renders an action button that fires the registered handler on click', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // Arm a handler whose invocation we can observe from the page side.
        await page.evaluate(() => {
            window.__toastActionFired = 0;
            window.__appTestHooks.registerToastActionHandler(
                'test-action',
                () => { window.__toastActionFired += 1; }
            );
            window.__appTestHooks.showActionToast({
                type: 'error',
                title: 'Session expired',
                detail: 'Log in again to continue.',
                action: { label: 'Log in again', type: 'test-action' }
            });
        });

        const toast = page.locator('.toast--error', { hasText: 'Session expired' });
        await expect(toast).toBeVisible({ timeout: 5_000 });

        const actionButton = toast.locator('.toast__action');
        await expect(actionButton).toBeVisible();
        await expect(actionButton).toHaveText('Log in again');

        await actionButton.click();

        const fired = await page.evaluate(() => window.__toastActionFired);
        expect(fired).toBe(1);
    });

    test('action-bearing toast does NOT auto-close past the 8 s error window', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        await page.evaluate(() => {
            window.__appTestHooks.showActionToast({
                type: 'error',
                title: 'Persistent toast',
                detail: null,
                action: { label: 'Act', type: 'persistent-noop' }
            });
        });

        const toast = page.locator('.toast', { hasText: 'Persistent toast' });
        await expect(toast).toBeVisible();

        // Poll for absence over a 9 s window — past the 8 s error auto-close.
        // If the toast goes hidden inside the window, the wait resolves and we
        // fail; if it times out (toast still visible), we treat that as the
        // success path.
        const wentHidden = await toast.waitFor({ state: 'hidden', timeout: 9_000 })
            .then(() => true)
            .catch(() => false);
        expect(wentHidden).toBe(false);
        await expect(toast).toBeVisible();
    });

    test('plain error toast without action still auto-closes', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        await page.evaluate(() => {
            window.__appTestHooks.showActionToast({
                type: 'error',
                title: 'Plain error',
                detail: null,
                action: null
            });
        });

        const toast = page.locator('.toast', { hasText: 'Plain error' });
        await expect(toast).toBeVisible();
        await expect(toast).toBeHidden({ timeout: 9_500 });
    });

});
