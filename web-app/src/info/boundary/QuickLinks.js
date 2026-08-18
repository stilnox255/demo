import BElement from "../../BElement.js";
import { html } from "lit-html";

class QuickLinks extends BElement {
    extractState() {
        return {};
    }

    view() {
        return html`
            <section class="config-section">
                <h2>Quick Links</h2>
                <nav aria-label="API endpoints">
                    <ul>
                        <li><a href="/api/demo-items">Demo Items API</a></li>
                        <li><a href="/q/openapi">OpenAPI document</a></li>
                        <li><a href="/q/swagger-ui">Swagger UI</a></li>
                        <li><a href="/.well-known/app-config">App config</a></li>
                    </ul>
                </nav>
            </section>
        `;
    }
}

customElements.define("quick-links", QuickLinks);
