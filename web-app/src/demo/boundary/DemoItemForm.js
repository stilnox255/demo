import BElement from "../../BElement.js";
import { html } from "lit-html";
import { createDemoItem } from "../control/DemoItemsControl.js";

/**
 * Create form for a demo item.
 *
 * Validation is left to the platform: `required` and `maxlength` on the inputs
 * give native messages in the user's language, and the server rejects anything
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
                    Name
                    <input name="name" type="text" required maxlength="120" placeholder="What to call it">
                </label>
                <label>
                    Description
                    <input name="description" type="text" maxlength="2000" placeholder="Optional">
                </label>
                <button type="submit" ?disabled=${this.state.isLoading}>Create</button>
            </form>
        `;
    }
}

customElements.define("demo-item-form", DemoItemForm);
