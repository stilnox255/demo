import BElement from "../../BElement.js";
import { html } from "lit-html";
import { login, performLogout } from "../control/AuthControl.js";

class KeycloakAuth extends BElement {
    extractState({ auth }) {
        return auth;
    }

    view() {
        if (this.state.isLoading) {
            return html`<div class="auth-box"><p>Loading...</p></div>`;
        }

        if (!this.state.isAuthenticated) {
            return html`
                <div class="auth-box">
                    <p>Login Required</p>
                    <button class="primary-button" @click=${login}>Login</button>
                </div>
            `;
        }

        const username = this.state.userInfo?.preferred_username || 
                         this.state.userInfo?.email || 
                         "User";

        return html`
            <div class="auth-status">
                <span>Logged in as: <strong>${username}</strong></span>
                <button class="secondary-button" @click=${performLogout}>Logout</button>
            </div>
        `;
    }
}

customElements.define("keycloak-auth", KeycloakAuth);
