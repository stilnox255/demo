import { test, expect } from '@playwright/test';

// The demo resource end to end, through the real UI against the real backend.
//
// What only an end-to-end test can catch: that the form, the control layer, the
// store, the table and the API agree. Each of those has its own unit coverage and
// they can all pass while the wiring between them is wrong.
//
// Every item is created with a unique name so specs can run in any order and
// against a database another spec has already written to.
test.describe('Demo items', () => {

    const uniqueName = () => `e2e item ${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

    test('creates an item and shows it in the table', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        const name = uniqueName();
        await page.fill('.demo-item-form input[name="name"]', name);
        await page.fill('.demo-item-form input[name="description"]', 'created by an e2e run');
        await page.click('.demo-item-form button[type="submit"]');

        // The success toast is explicit for a create (ADR-31) and the row is the
        // second half of the same assertion: the list reloaded after the write.
        await expect(page.locator('.toast--success', { hasText: 'Item created' })).toBeVisible({ timeout: 10_000 });
        await expect(page.locator('table.data-table tbody tr', { hasText: name })).toBeVisible();
    });

    test('rejects an empty name without reaching the server', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        // `required` on the input, so the browser blocks the submit. No request, no
        // toast — the platform's own validation is the first line (ADR-09).
        await page.click('.demo-item-form button[type="submit"]');

        await expect(page.locator('.toast--error')).toHaveCount(0);
        await expect(page.locator('.demo-item-form input[name="name"]:invalid')).toHaveCount(1);
    });

    test('activates an item and the version increments', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        const name = uniqueName();
        await page.fill('.demo-item-form input[name="name"]', name);
        await page.click('.demo-item-form button[type="submit"]');

        const row = page.locator('table.data-table tbody tr', { hasText: name });
        await expect(row).toBeVisible({ timeout: 10_000 });
        await expect(row.locator('td').nth(1)).toHaveText('DRAFT');
        await expect(row.locator('td').nth(3)).toHaveText('0');

        await row.getByRole('button', { name: 'Activate' }).click();

        // The version moving is the visible proof that the optimistic-lock token
        // round-tripped: the client sent what it read and the server accepted it.
        await expect(row.locator('td').nth(1)).toHaveText('ACTIVE', { timeout: 10_000 });
        await expect(row.locator('td').nth(3)).toHaveText('1');
    });

    test('deletes an item', async ({ page }) => {
        await page.goto('/');
        await page.waitForSelector('nav.sidebar', { timeout: 20_000 });

        const name = uniqueName();
        await page.fill('.demo-item-form input[name="name"]', name);
        await page.click('.demo-item-form button[type="submit"]');

        const row = page.locator('table.data-table tbody tr', { hasText: name });
        await expect(row).toBeVisible({ timeout: 10_000 });

        await row.getByRole('button', { name: 'Delete' }).click();

        await expect(row).toHaveCount(0, { timeout: 10_000 });
    });
});
