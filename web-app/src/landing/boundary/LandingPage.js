import BElement from "../../BElement.js";
import { html } from "lit-html";
import { login } from "../../auth/control/AuthControl.js";
import { t } from "../../i18n/control/I18nControl.js";

/**
 * Unauthenticated entry point. Rendered by the shell whenever there is no valid
 * session, so it doubles as the sign-out destination — there is no separate
 * logged-out route to keep in sync.
 */
class LandingPage extends BElement {
    extractState({ auth: { isLoading } }) {
        return { isLoading };
    }

    view() {
        return html`
            <div class="landing">
                <div class="landing-grid-overlay" aria-hidden="true"></div>

                <div class="landing-hero">
                    <div class="landing-brand">
                        <div class="landing-logo" aria-hidden="true">
                            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <rect x="4" y="4" width="56" height="56" rx="6" stroke="currentColor" stroke-width="2"/>
                                <rect x="16" y="18" width="32" height="4" rx="2" fill="currentColor"/>
                                <rect x="16" y="30" width="32" height="4" rx="2" fill="currentColor" opacity="0.7"/>
                                <rect x="16" y="42" width="20" height="4" rx="2" fill="currentColor" opacity="0.45"/>
                            </svg>
                        </div>
                        <h1>${t("app.title")}</h1>
                        <p class="landing-tagline">${t("landing.tagline")}</p>
                    </div>

                    <button
                        class="landing-login-btn"
                        @click=${login}
                        ?disabled=${this.state.isLoading}
                        aria-label=${t("landing.signInAria")}
                    >
                        ${this.state.isLoading ? t("landing.initializing") : t("landing.signIn")}
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                            <polyline points="10 17 15 12 10 7"/>
                            <line x1="15" y1="12" x2="3" y2="12"/>
                        </svg>
                    </button>
                </div>

                <div class="landing-features" role="list" aria-label=${t("landing.featuresAria")}>
                    <article class="landing-feature-card" role="listitem">
                        <div class="feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="8" y1="6" x2="21" y2="6"/>
                                <line x1="8" y1="12" x2="21" y2="12"/>
                                <line x1="8" y1="18" x2="21" y2="18"/>
                                <line x1="3" y1="6" x2="3.01" y2="6"/>
                                <line x1="3" y1="12" x2="3.01" y2="12"/>
                                <line x1="3" y1="18" x2="3.01" y2="18"/>
                            </svg>
                        </div>
                        <h2>${t("landing.feature.demo.title")}</h2>
                        <p>${t("landing.feature.demo.body")}</p>
                    </article>

                    <article class="landing-feature-card" role="listitem">
                        <div class="feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                            </svg>
                        </div>
                        <h2>${t("landing.feature.status.title")}</h2>
                        <p>${t("landing.feature.status.body")}</p>
                    </article>

                    <article class="landing-feature-card" role="listitem">
                        <div class="feature-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                <polyline points="14 2 14 8 20 8"/>
                                <line x1="16" y1="13" x2="8" y2="13"/>
                                <line x1="16" y1="17" x2="8" y2="17"/>
                            </svg>
                        </div>
                        <h2>${t("landing.feature.api.title")}</h2>
                        <p>${t("landing.feature.api.body")}</p>
                    </article>
                </div>

                <footer class="landing-footer">
                    <p>${t("app.footer")}</p>
                </footer>
            </div>
        `;
    }
}

customElements.define("b-landing-page", LandingPage);
