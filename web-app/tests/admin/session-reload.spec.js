import { test, expect } from '@playwright/test';

test.describe('Session reload after access-token expiry', () => {
    test('refreshes silently and stays authenticated', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // Mutate access token to an expired JWT with same header/signature shape.
        await page.evaluate(() => {
            const raw = localStorage.getItem('access_token');
            const [header, payloadB64, signature] = raw.split('.');

            const b64UrlToB64 = (s) => s.replace(/-/g, '+').replace(/_/g, '/').padEnd(s.length + (4 - s.length % 4) % 4, '=');
            const b64ToB64Url = (s) => s.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

            const payload = JSON.parse(atob(b64UrlToB64(payloadB64)));
            payload.exp = Math.floor(Date.now() / 1000) - 60;

            const expiredPayload = b64ToB64Url(btoa(JSON.stringify(payload)));
            localStorage.setItem('access_token', `${header}.${expiredPayload}.${signature}`);
        });

        await page.reload();
        await expect(page.locator('nav.sidebar')).toBeVisible({ timeout: 15_000 });
        await expect(page.locator('.landing-login-btn')).toHaveCount(0);
        await expect(page).not.toHaveURL(/\/realms\/starter\/protocol\/openid-connect\/auth/);
    });
});
