import { test, expect } from '@playwright/test';

// E29 T-06 — when the access token is invalid AND the refresh token is gone
// or invalid, authenticatedFetch must surface a non-blocking "Session expired"
// toast with a "Log in again" action button instead of redirecting to
// Keycloak. The page stays on the current URL until the user clicks.

test.describe('Session-expired toast on hard refresh failure', () => {

    test('refresh failure shows session-expired toast with login action', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // Force the next authenticated call to hit the 401 → refresh-fail path:
        // expired access token + an invalid refresh token Keycloak will reject.
        await page.evaluate(() => {
            localStorage.setItem('access_token', 'header.eyJleHAiOjB9.sig');
            localStorage.setItem('refresh_token', 'invalid-refresh');
        });

        // Trigger any authenticated call by navigating to a page that fetches.
        await page.goto('/status');

        const toast = page.locator('.toast--error', { hasText: 'Session expired' });
        await expect(toast).toBeVisible({ timeout: 10_000 });

        // Detail line and action button are both rendered.
        await expect(toast).toContainText('Log in again to continue');
        const actionButton = toast.locator('.toast__action', { hasText: 'Log in again' });
        await expect(actionButton).toBeVisible();

        // The page did NOT auto-redirect to Keycloak — toast is the only signal.
        await expect(page).not.toHaveURL(/\/realms\/starter\/protocol\/openid-connect\/auth/);
    });

    test('repeated refresh failures do not stack additional toasts', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        await page.evaluate(() => {
            localStorage.setItem('access_token', 'header.eyJleHAiOjB9.sig');
            localStorage.setItem('refresh_token', 'invalid-refresh');
        });

        // First trigger.
        await page.goto('/status');
        const toast = page.locator('.toast--error', { hasText: 'Session expired' });
        await expect(toast).toBeVisible({ timeout: 10_000 });

        // Second trigger while the first toast is still visible. The repeat
        // suppression in sessionExpired() must keep the count at exactly 1.
        await page.goto('/');

        // Give the second call a moment to complete; assert the count stays 1.
        await page.waitForTimeout(1_500);
        await expect(page.locator('.toast--error', { hasText: 'Session expired' })).toHaveCount(1);
    });
});
