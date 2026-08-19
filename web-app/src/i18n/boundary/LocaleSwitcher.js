import BElement from "../../BElement.js";
import { html } from "lit-html";
import { toggleLocale, t, languageName, nextLocale } from "../control/I18nControl.js";

/**
 * Locale toggle, modelled on `ThemeSwitcher`: a cycle button rather than a
 * dropdown, because two locales do not earn a menu.
 *
 * The visible label is the *current* locale, the accessible label names the one
 * a click switches to — the button shows state, the label describes the action.
 */
class LocaleSwitcher extends BElement {
    extractState({ i18n }) {
        return i18n;
    }

    view() {
        return html`
            <button class="theme-toggle locale-toggle"
                    aria-label=${t("locale.switch", { language: languageName(nextLocale()) })}
                    @click=${toggleLocale}>
                ${this.state.locale?.toUpperCase() ?? ""}
            </button>
        `;
    }
}

customElements.define("locale-switcher", LocaleSwitcher);
