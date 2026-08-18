import BElement from "../../BElement.js";
import { html } from "lit-html";

class ApiDocumentation extends BElement {
    extractState() {
        return {};
    }

    view() {
        return html`
            <section class="config-section">
                <h2>API Documentation</h2>
                <details>
                    <summary>OpenAPI Specification</summary>
                    <p>View API documentation at <a href="/openapi" target="_blank">openapi</a></p>
                    <p>Swagger UI at <a href="/q/swagger-ui" target="_blank">/q/swagger-ui</a></p>
                </details>
            </section>
        `;
    }
}

customElements.define("api-documentation", ApiDocumentation);
