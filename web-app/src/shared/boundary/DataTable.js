import BElement from "../../BElement.js";
import { html } from "lit-html";
import "./PaginationControls.js";
import { t } from "../../i18n/control/I18nControl.js";

class DataTable extends BElement {

    extractState() {
        return {};
    }

    view() {
        const columns = this.columns || [];
        const rows = this.rows || [];
        const renderRow = this.renderRow || (() => html``);
        const rowClass = this.rowClass || (() => "");
        const page = Number(this.getAttribute("page") || 1);
        const pageSize = Number(this.getAttribute("page-size") || 25);
        const totalPages = Number(this.getAttribute("total-pages") || 1);
        const totalItems = Number(this.getAttribute("total-items") || 0);

        if (rows.length === 0) {
            return html`<p class="empty-state">${t("table.noData")}</p>`;
        }

        return html`
            <table class="data-table">
                <thead>
                    <tr>
                        ${columns.map(col => html`<th>${col}</th>`)}
                    </tr>
                </thead>
                <tbody>
                    ${rows.map(row => {
                        const cls = rowClass(row);
                        return cls
                            ? html`<tr class=${cls}>${renderRow(row)}</tr>`
                            : html`<tr>${renderRow(row)}</tr>`;
                    })}
                </tbody>
            </table>
            ${totalPages > 1 ? html`
                <app-pagination
                    page=${page}
                    page-size=${pageSize}
                    total-pages=${totalPages}
                    total-items=${totalItems}>
                </app-pagination>
            ` : ""}
        `;
    }
}

customElements.define("app-data-table", DataTable);
