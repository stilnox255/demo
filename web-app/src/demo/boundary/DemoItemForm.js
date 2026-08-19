import BElement from "../../BElement.js";
import { html } from "lit-html";
import { createDemoItem } from "../control/DemoItemsControl.js";
import { t } from "../../i18n/control/I18nControl.js";

/**
 * Create form for a demo item.
 *
 * Validation is left to the platform: `required` and `maxlength` on the inputs
 * give native messages in the browser's language — the one part of the UI that
 * was already localized before the catalogue existed, and the reason there is no
 * `setCustomValidity` here. The server rejects anything
 * that gets past them anyway. Re-implementing the checks in JavaScript would add
 * a second set of rules to keep in sync with the backend's.
 */
class DemoItemForm extends BElement {
    extractState({ demoItems: { isLoading } }) {
        return { isLoading };
    }

    async submit(event) {
        event.preventDefault();
        const form = event.target;
        const created = await createDemoItem(form.name.value, form.description.value);
        if (created) {
            form.reset();
        }
    }

    view() {
        return html`
            <form class="demo-item-form" @submit=${(e) => this.submit(e)}>
                <label>
                    ${t("demo.form.name")}
                    <input name="name" type="text" required maxlength="120"
                           placeholder=${t("demo.form.namePlaceholder")}>
                </label>
                <label>
                    ${t("demo.form.description")}
                    <input name="description" type="text" maxlength="2000"
                           placeholder=${t("demo.form.descriptionPlaceholder")}>
                </label>
                <button type="submit" ?disabled=${this.state.isLoading}>${t("demo.form.create")}</button>
            </form>
        `;
    }
}

customElements.define("demo-item-form", DemoItemForm);
