import { USERS } from './users.js';

// Performs the real Keycloak login for the given role and persists the
// authenticated browser state (tokens live in localStorage) to
// tests/.auth/<role>.json for reuse via storageState.
export async function authenticate(page, role) {
    const user = USERS[role];
    if (!user) throw new Error(`Unknown role: ${role}`);

    // Landing page. The Sign In button stays disabled until the app and OIDC config
    // are loaded; click auto-waits for it to become enabled.
    await page.goto('/');
    await page.click('.landing-login-btn', { timeout: 20_000 });

    // Keycloak login form.
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', user.username);
    await page.fill('input[name="password"]', user.password);
    await page.click('input[type="submit"], button[type="submit"]');

    // callback.html exchanges the code, stores tokens, and redirects to '/'.
    // The authenticated shell renders the sidebar nav.
    await page.waitForSelector('nav.sidebar', { timeout: 20_000 });
    await page.context().storageState({ path: `tests/.auth/${role}.json` });
}
