import BElement from "../../BElement.js";
import { html } from "lit-html";
import "../../shared/boundary/DataTable.js";
import "./DemoItemForm.js";
import {
    loadDemoItems,
    updateDemoItemStatus,
    deleteDemoItem,
    attachFileToDemoItem,
} from "../control/DemoItemsControl.js";
import { t } from "../../i18n/control/I18nControl.js";

/**
 * The demo resource, end to end: paginated list, create form, status change with
 * optimistic locking, file attachment and a signed download link.
 *
 * Reads its data from the store and calls the control layer for every action —
 * no fetch and no dispatch in this file (ADR-28).
 */
class DemoItemsView extends BElement {
    connectedCallback() {
        super.connectedCallback();
        // After the first render, so the table has somewhere to appear.
        this._initTimeout = setTimeout(() => loadDemoItems(), 0);
        this._onPageChange = (event) => loadDemoItems(event.detail.page, this.state.meta.pageSize);
        this.addEventListener("page-change", this._onPageChange);
    }

    disconnectedCallback() {
        clearTimeout(this._initTimeout);
        this.removeEventListener("page-change", this._onPageChange);
        super.disconnectedCallback();
    }

    extractState({ demoItems }) {
        return demoItems;
    }

    view() {
        const { items, meta, isLoading } = this.state;

        return html`
            <section class="config-section">
                <h2>${t("demo.items.title")}</h2>
                <demo-item-form></demo-item-form>

                ${isLoading && items.length === 0 ? html`<p class="empty-state">${t("demo.items.loading")}</p>` : ""}

                <app-data-table
                    .columns=${[t("demo.column.name"), t("demo.column.status"), t("demo.column.attachment"), t("demo.column.version"), ""]}
                    .rows=${items}
                    .renderRow=${(item) => this.renderRow(item)}
                    page=${meta.page}
                    page-size=${meta.pageSize}
                    total-pages=${meta.totalPages}
                    total-items=${meta.totalItems}>
                </app-data-table>
            </section>
        `;
    }

    renderRow(item) {
        return html`
            <td>${item.name}</td>
            <td>${t(`demo.status.${item.status}`)}</td>
            <td>${this.renderAttachment(item)}</td>
            <td>${item.version}</td>
            <td class="row-actions">
                ${this.renderStatusButton(item)}
                <label class="file-button">
                    ${t("demo.attach")}
                    <input type="file" hidden @change=${(e) => this.attach(item, e)}>
                </label>
                <button @click=${() => deleteDemoItem(item)}>${t("demo.delete")}</button>
            </td>
        `;
    }

    /**
     * The download URL is signed and short-lived, which is what lets a plain link
     * work without an Authorization header. It is rendered from the item the
     * server just sent rather than cached anywhere — a stored one expires.
     */
    renderAttachment(item) {
        if (!item.attachment) {
            return html`<span class="muted">${t("demo.noAttachment")}</span>`;
        }
        return html`<a href=${item.attachment.downloadUrl} download>${item.attachment.fileName}</a>`;
    }

    /**
     * The branch runs on the raw status the API sent, not on a translated label —
     * the enum is a protocol value, the label is presentation. Mixing the two
     * makes the transition depend on the current locale.
     */
    renderStatusButton(item) {
        if (item.status === "ARCHIVED") {
            return "";
        }
        const next = item.status === "DRAFT" ? "ACTIVE" : "ARCHIVED";
        const label = next === "ACTIVE" ? t("demo.activate") : t("demo.archive");
        return html`<button @click=${() => updateDemoItemStatus(item, next)}>${label}</button>`;
    }

    async attach(item, event) {
        const file = event.target.files?.[0];
        if (file) {
            await attachFileToDemoItem(item, file);
        }
        event.target.value = "";
    }
}

customElements.define("demo-items-view", DemoItemsView);
