import { test, expect } from '@playwright/test';

// Locale switching end to end.
//
// The mechanism has no reactivity of its own: `t()` reads a module-level
// catalogue, and it is the Redux dispatch in `localeChanged` that re-renders every
// mounted component. Only a real browser proves that chain actually reaches the
// DOM, including the parts that are not plain template text — the `lang`
// attribute, and a toast whose text lives in the store rather than in a template.
test.describe('Locale switch', () => {

    const localeToggle = (page) => page.locator('locale-switcher button');

    test('switches the UI language and the document language', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // The project pins navigator.language to en-US, so this is the detected default.
        await expect(page.locator('.config-section h2')).toHaveText('Demo Items');
        await expect(page.locator('html')).toHaveAttribute('lang', 'en');
        await expect(localeToggle(page)).toHaveText('EN');

        await localeToggle(page).click();

        await expect(page.locator('.config-section h2')).toHaveText('Demo-Objekte');
        await expect(page.locator('html')).toHaveAttribute('lang', 'de');
        await expect(localeToggle(page)).toHaveText('DE');
    });

    test('keeps the chosen language across a reload', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        await localeToggle(page).click();
        await expect(page.locator('.config-section h2')).toHaveText('Demo-Objekte');

        await page.reload();
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // Persistence rides on the whole-state localStorage snapshot, and the
        // rehydrated value has to beat browser detection on the next boot.
        await expect(page.locator('.config-section h2')).toHaveText('Demo-Objekte');
        await expect(page.locator('html')).toHaveAttribute('lang', 'de');
    });

    test('re-translates a toast that is already on screen', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // An action toast persists until dismissed, so there is no auto-close race.
        // It carries catalogue keys, which is the point of the test: the text is
        // resolved when the toast renders, not when it was raised.
        await page.evaluate(() => window.__appTestHooks.showActionToast({
            type: 'error',
            title: 'auth.sessionExpired.title',
            detail: 'auth.sessionExpired.detail',
            action: { label: 'auth.sessionExpired.action', type: 'login' }
        }));

        const toast = page.locator('.toast--error');
        await expect(toast).toContainText('Session expired');
        await expect(toast.locator('.toast__action')).toHaveText('Log in again');

        await localeToggle(page).click();

        await expect(toast).toContainText('Sitzung abgelaufen');
        await expect(toast).toContainText('Zum Fortfahren erneut anmelden');
        await expect(toast.locator('.toast__action')).toHaveText('Erneut anmelden');
    });
});
