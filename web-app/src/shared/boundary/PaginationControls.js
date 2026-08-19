import BElement from "../../BElement.js";
import { html } from "lit-html";
import { t, formatNumber } from "../../i18n/control/I18nControl.js";

/**
 * Generic pagination web component for all paginated lists.
 * Renders page navigation based on PageMeta from the backend's PaginatedResponse.
 *
 * Properties: page, pageSize, totalPages, totalItems
 * Events: page-change ({ detail: { page } }) — emitted when the user navigates
 */
class PaginationControls extends BElement {

    extractState() {
        return {};
    }

    view() {
        const page = Number(this.getAttribute("page") || 1);
        const totalPages = Number(this.getAttribute("total-pages") || 1);
        const totalItems = Number(this.getAttribute("total-items") || 0);
        const pageSize = Number(this.getAttribute("page-size") || 25);

        const isFirst = page <= 1;
        const isLast = page >= totalPages;
        const start = Math.min((page - 1) * pageSize + 1, totalItems);
        const end = Math.min(page * pageSize, totalItems);

        return html`
            <div class="pagination">
                <button
                    ?disabled=${isFirst}
                    @click=${() => this.navigate(page - 1)}>
                    ${t("pagination.previous")}
                </button>
                <span class="pagination-info">
                    ${t("pagination.range", {
                        start: formatNumber(start),
                        end: formatNumber(end),
                        total: formatNumber(totalItems),
                        page: formatNumber(page),
                        totalPages: formatNumber(totalPages)
                    })}
                </span>
                <button
                    ?disabled=${isLast}
                    @click=${() => this.navigate(page + 1)}>
                    ${t("pagination.next")}
                </button>
            </div>
        `;
    }

    navigate(page) {
        this.dispatchEvent(new CustomEvent("page-change", {
            detail: { page },
            bubbles: true,
            composed: true
        }));
    }
}

customElements.define("app-pagination", PaginationControls);
