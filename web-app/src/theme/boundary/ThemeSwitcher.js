import BElement from "../../BElement.js";
import { html } from "lit-html";
import { toggleTheme } from "../control/ThemeControl.js";
import { t } from "../../i18n/control/I18nControl.js";

class ThemeSwitcher extends BElement {
    extractState({ theme }) {
        return theme;
    }

    view() {
        const iconName = this.state.theme === "dark" ? "🌙" : this.state.theme === "light" ? "☀️" : "🖥️";
        
        return html`
            <button class="theme-toggle"
                    aria-label=${t("theme.toggle")}
                    @click=${toggleTheme}>
                ${iconName}
            </button>
        `;
    }
}

customElements.define("theme-switcher", ThemeSwitcher);
