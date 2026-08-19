import { defineConfig, devices } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080';

export default defineConfig({
    testDir: './tests',
    timeout: 30_000,
    retries: 0,
    // The locale is pinned, not inherited: the app picks its language from
    // navigator.language when the user has not chosen one, so every spec that
    // asserts UI text would otherwise depend on the host's language. locale-switch
    // .spec.js is the one place that changes it, deliberately.
    use: { baseURL: BASE_URL, locale: 'en-US' },
    // Reuse a running quarkusDev stack; start one only if nothing answers on BASE_URL.
    // In CI always start a fresh stack (reuseExistingServer: false).
    webServer: {
        command: 'cd .. && ./gradlew quarkusDev',
        url: BASE_URL,
        reuseExistingServer: !process.env.CI,
        timeout: 240_000,
        stdout: 'pipe',
        stderr: 'pipe'
    },
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
