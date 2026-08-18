import BElement from "../../BElement.js";
import { html } from "lit-html";
import { dismissToast, runToastAction } from "../control/NotificationsControl.js";

class ToastContainer extends BElement {
    extractState({ notifications }) {
        return notifications.toasts;
    }
    triggerViewUpdate() {
        super.triggerViewUpdate();
        this._scheduleAutoClose();
    }
    view() {
        return html`
            <div class="toast-stack" role="log" aria-live="polite" aria-label="Notifications">
                ${this.state.map(toast => html`
                    <div class="toast toast--${toast.type}" role="alert">
                        <div class="toast__body">
                            <span class="toast__title">${toast.title}</span>
                            ${toast.detail ? html`<span class="toast__detail">${toast.detail}</span>` : ""}
                        </div>
                        ${toast.action ? html`
                            <button
                                class="toast__action"
                                @click="${_ => { runToastAction(toast.action.type); dismissToast(toast.id); }}"
                            >${toast.action.label}</button>
                        ` : ""}
                        <button class="toast__close" aria-label="Dismiss" @click="${_ => dismissToast(toast.id)}">✕</button>
                    </div>
                `)}
            </div>
        `;
    }
    connectedCallback() {
        super.connectedCallback();
        this._timers = new Map();
    }
    _scheduleAutoClose() {
        if (!this.state) return;
        this.state.forEach(toast => {
            if (this._timers.has(toast.id)) return;
            // Toasts that carry an action persist until the user clicks the
            // action or the dismiss button — no auto-close timer.
            if (toast.action) return;
            const delay = toast.type === "success" ? 4000 : 8000;
            const timer = setTimeout(() => dismissToast(toast.id), delay);
            this._timers.set(toast.id, timer);
        });
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        this._timers?.forEach(t => clearTimeout(t));
    }
}
customElements.define("toast-container", ToastContainer);
