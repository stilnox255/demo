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
                <h2>Demo Items</h2>
                <demo-item-form></demo-item-form>

                ${isLoading && items.length === 0 ? html`<p class="empty-state">Loading…</p>` : ""}

                <app-data-table
                    .columns=${["Name", "Status", "Attachment", "Version", ""]}
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
            <td>${item.status}</td>
            <td>${this.renderAttachment(item)}</td>
            <td>${item.version}</td>
            <td class="row-actions">
                ${this.renderStatusButton(item)}
                <label class="file-button">
                    Attach
                    <input type="file" hidden @change=${(e) => this.attach(item, e)}>
                </label>
                <button @click=${() => deleteDemoItem(item)}>Delete</button>
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
            return html`<span class="muted">—</span>`;
        }
        return html`<a href=${item.attachment.downloadUrl} download>${item.attachment.fileName}</a>`;
    }

    renderStatusButton(item) {
        if (item.status === "ARCHIVED") {
            return "";
        }
        const next = item.status === "DRAFT" ? "ACTIVE" : "ARCHIVED";
        return html`<button @click=${() => updateDemoItemStatus(item, next)}>${next === "ACTIVE" ? "Activate" : "Archive"}</button>`;
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
