import { defineConfig, devices } from '@playwright/test';

// The app under test is the single-origin setup: the dev proxy on :80 routes /api,
// /q and /.well-known to Quarkus and everything else to Vite. Pointing this at the
// backend directly cannot work — the backend has no static resources and no catch-all
// route (ADR-25), so it answers 404 on `/`, and Playwright's readiness check treats
// that as "not up yet" and waits out its timeout.
// Set BASE_URL when DEV_PROXY_PORT moves the proxy off :80.
const BASE_URL = process.env.BASE_URL ?? 'http://localhost';
const API_URL = process.env.API_URL ?? 'http://localhost:8080';

export default defineConfig({
    testDir: './tests',
    timeout: 30_000,
    retries: 0,
    // The locale is pinned, not inherited: the app picks its language from
    // navigator.language when the user has not chosen one, so every spec that
    // asserts UI text would otherwise depend on the host's language. locale-switch
    // .spec.js is the one place that changes it, deliberately.
    use: { baseURL: BASE_URL, locale: 'en-US' },
    // Both halves of the single origin, started in order and each waited for.
    // Either is reused if it is already running, so the common case — a dev session
    // with both already up — starts nothing.
    //
    // The backend is probed on a health endpoint rather than on `/`, for the reason
    // in the BASE_URL comment. quarkusDev also brings up the dev proxy, so :80 only
    // starts answering once Vite is behind it, which is what the second entry waits
    // for.
    webServer: [
        {
            command: 'cd .. && ./gradlew quarkusDev',
            url: `${API_URL}/q/health/ready`,
            reuseExistingServer: !process.env.CI,
            timeout: 240_000,
            stdout: 'pipe',
            stderr: 'pipe'
        },
        {
            command: 'npm run dev',
            url: BASE_URL,
            reuseExistingServer: !process.env.CI,
            timeout: 60_000,
            stdout: 'pipe',
            stderr: 'pipe'
        }
    ],
    // One project per user group. To add a role: create tests/setup/<role>.setup.js,
    // a tests/<role>/ folder of specs, an entry in tests/users.js, and a project pair below.
    projects: [
        { name: 'setup-admin', testMatch: /setup\/admin\.setup\.js/ },
        {
            name: 'admin',
            testDir: './tests/admin',
            use: { ...devices['Desktop Chrome'], storageState: 'tests/.auth/admin.json' },
            dependencies: ['setup-admin']
        }
    ]
});
