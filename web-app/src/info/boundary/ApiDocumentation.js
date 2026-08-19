import BElement from "../../BElement.js";
import { html } from "lit-html";
import { t } from "../../i18n/control/I18nControl.js";

class ApiDocumentation extends BElement {
    extractState() {
        return {};
    }

    view() {
        return html`
            <section class="config-section">
                <h2>${t("api.title")}</h2>
                <details>
                    <summary>${t("api.specification")}</summary>
                    <p>${t("api.viewDocumentationAt")} <a href="/openapi" target="_blank">openapi</a></p>
                    <p>${t("api.swaggerUiAt")} <a href="/q/swagger-ui" target="_blank">/q/swagger-ui</a></p>
                </details>
            </section>
        `;
    }
}

customElements.define("api-documentation", ApiDocumentation);
