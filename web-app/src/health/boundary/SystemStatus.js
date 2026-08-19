import BElement from "../../BElement.js";
import { html } from "lit-html";
import { startHealthPolling, stopHealthPolling } from "../control/HealthControl.js";
import { t } from "../../i18n/control/I18nControl.js";

class SystemStatus extends BElement {
    constructor() {
        super();
        this.pollInterval = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._initTimeout = setTimeout(() => {
            this.pollInterval = startHealthPolling();
        }, 0);
    }

    disconnectedCallback() {
        clearTimeout(this._initTimeout);
        stopHealthPolling(this.pollInterval);
        super.disconnectedCallback();
    }

    extractState({ health }) {
        return health;
    }

    view() {
        const statusClass = this.state.status.toLowerCase();
        const statusText = this.state.status === "UP"
            ? t("status.up")
            : this.state.status === "DOWN"
            ? t("status.down")
            : t("status.checking");

        return html`
            <section class="status-page">
                <div class="status-header">
                    <div class="status-badge ${statusClass}">
                        <span class="status-icon">●</span>
                        <span class="status-text">${statusText}</span>
                    </div>
                </div>
                
                <div class="services-list">
                    ${this.renderServices()}
                </div>
                
                <details class="status-details">
                    <summary>${t("status.technicalDetails")}</summary>
                    <pre>${JSON.stringify({ status: this.state.status, checks: this.state.checks }, null, 2)}</pre>
                </details>
            </section>
        `;
    }

    renderServices() {
        // Mirrors what the backend actually reports on /q/health/ready. The cache is
        // deliberately absent: it is not part of readiness (see ADR-21), so a card
        // for it here would show UNKNOWN forever and teach everyone to ignore the
        // page.
        const services = [
            { id: "database", icon: "💾" },
            { id: "s3", icon: "🗄️" }
        ];

        return services.map(service => {
            const check = this.findCheck(service.id);
            const status = check ? check.status : "UNKNOWN";
            const statusClass = status.toLowerCase();

            return html`
                <div class="service-item">
                    <span class="service-icon">${service.icon}</span>
                    <div class="service-info">
                        <h3>${t(`status.service.${service.id}.name`)}</h3>
                        <p>${t(`status.service.${service.id}.description`)}</p>
                    </div>
                    <div class="service-status ${statusClass}">${t(`status.label.${status}`)}</div>
                </div>
            `;
        });
    }

    findCheck(serviceId) {
        return this.state.checks.find(check => {
            const lower = check.name.toLowerCase();
            return lower.includes(serviceId) || 
                   (serviceId === "database" && (lower.includes("datasource") || lower.includes("db"))) ||
                   (serviceId === "s3" && lower.includes("storage"));
        });
    }
}

customElements.define("system-status", SystemStatus);
