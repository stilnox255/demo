import BElement from "../../BElement.js";
import { html } from "lit-html";
import { Router } from "@vaadin/router";
import "../../auth/boundary/KeycloakAuth.js";
import "../../theme/boundary/ThemeSwitcher.js";
import "../../i18n/boundary/LocaleSwitcher.js";
import "../../landing/boundary/LandingPage.js";
import "../../notification/boundary/ToastContainer.js";
import "../../health/boundary/SystemStatus.js";
import "../../info/boundary/ApiDocumentation.js";
import "../../demo/boundary/DemoItemsView.js";
import { hasRole } from "../../auth/control/AuthControl.js";
import { t } from "../../i18n/control/I18nControl.js";

let routerInitialized = false;

/**
 * Application shell: the authenticated frame and the router that fills it.
 *
 * The router is created once, on the first render where the user is
 * authenticated. Instantiating it before that would attach it to an outlet the
 * landing page has not yet made room for, and instantiating it on every render
 * would stack one router per state change.
 */
class AppShell extends BElement {
    extractState({ auth: { isAuthenticated, isLoading } }) {
        return { isAuthenticated, isLoading };
    }

    handleNavClick(e) {
        if (e.target.tagName === "A") {
            const toggle = this.querySelector("#menu-toggle");
            if (toggle) toggle.checked = false;
        }
    }

    triggerViewUpdate() {
        super.triggerViewUpdate();
        if (this.state?.isAuthenticated && !routerInitialized) {
            routerInitialized = true;
            const outlet = this.querySelector(".view");
            if (outlet) {
                const router = new Router(outlet, {});
                router.setRoutes([
                    { path: "/", component: "demo-items-view" },
                    { path: "/demo-items", redirect: "/" },
                    { path: "/status", component: "system-status" },
                    { path: "/api", component: "api-documentation" },
                ]);
            }
        }
    }

    view() {
        if (!this.state?.isAuthenticated) {
            return html`<b-landing-page></b-landing-page>`;
        }

        return html`
            <toast-container></toast-container>
            <header>
                <h1>${t("app.title")}</h1>
                <div class="auth-status">
                    <locale-switcher></locale-switcher>
                    <theme-switcher></theme-switcher>
                    <keycloak-auth></keycloak-auth>
                </div>
            </header>

            <main id="main">
                <input type="checkbox" id="menu-toggle" class="menu-toggle">
                <label for="menu-toggle" class="menu-icon" aria-label=${t("nav.menu")}>
                    <span></span>
                    <span></span>
                    <span></span>
                </label>

                <nav class="sidebar" @click="${this.handleNavClick}">
                    <ul>
                        <li><a href="/" class="nav-section">📋 ${t("nav.demoItems")}</a></li>
                        <li><a href="/api" class="nav-sub">📖 ${t("nav.apiDocumentation")}</a></li>
                        ${hasRole("admin") ? html`<li><a href="/status" class="nav-sub">⚡ ${t("nav.systemStatus")}</a></li>` : ""}
                    </ul>
                </nav>

                <section class="view"></section>
            </main>

            <footer>
                <p>${t("app.footer")}</p>
            </footer>
        `;
    }
}

customElements.define("b-app-shell", AppShell);
