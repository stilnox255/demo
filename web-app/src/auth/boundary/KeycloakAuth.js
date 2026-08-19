import BElement from "../../BElement.js";
import { html } from "lit-html";
import { login, performLogout } from "../control/AuthControl.js";
import { t } from "../../i18n/control/I18nControl.js";

class KeycloakAuth extends BElement {
    extractState({ auth }) {
        return auth;
    }

    view() {
        if (this.state.isLoading) {
            return html`<div class="auth-box"><p>${t("auth.loading")}</p></div>`;
        }

        if (!this.state.isAuthenticated) {
            return html`
                <div class="auth-box">
                    <p>${t("auth.loginRequired")}</p>
                    <button class="primary-button" @click=${login}>${t("auth.login")}</button>
                </div>
            `;
        }

        const username = this.state.userInfo?.preferred_username ||
                         this.state.userInfo?.email ||
                         t("auth.userFallback");

        return html`
            <div class="auth-status">
                <span>${t("auth.loggedInAs")} <strong>${username}</strong></span>
                <button class="secondary-button" @click=${performLogout}>${t("auth.logout")}</button>
            </div>
        `;
    }
}

customElements.define("keycloak-auth", KeycloakAuth);
