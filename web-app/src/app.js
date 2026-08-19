import store from "./store.js";
import { initTheme } from "./theme/control/ThemeControl.js";
import { initI18n } from "./i18n/control/I18nControl.js";
import { loadAppConfig, loadOidcConfig, checkAuth, login } from "./auth/control/AuthControl.js";
import { registerToastActionHandler } from "./notification/control/NotificationsControl.js";
import "./navigation/boundary/AppShell.js";

// Test hook exposed so the session-multi-tab.spec.js Playwright spec can read
// Redux state cross-tab. Gated to attach only under (a) Vite DEV builds or
// (b) when running on localhost/127.0.0.1 — covers both the local dev server
// and the Playwright run against Quarkus dev. Deployed production gets
// neither condition → hook is not installed.
if (typeof window !== "undefined") {
    const isDev = import.meta.env?.DEV;
    const isLocalhost = window.location?.hostname === "localhost" || window.location?.hostname === "127.0.0.1";
    if (isDev || isLocalhost) {
        window.__appStore = store;
    }
}

initTheme();
// Before the first render, like initTheme: catalogues are bundled, so this is
// synchronous and no frame ever paints untranslated keys.
initI18n();
registerToastActionHandler("login", login);

async function initApp() {
    const appConfig = await loadAppConfig();
    await loadOidcConfig(appConfig.authConfig.issuer);
    await checkAuth();
}
initApp();
