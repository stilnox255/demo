import BElement from "../../BElement.js";
import { html } from "lit-html";
import { toggleTheme } from "../control/ThemeControl.js";

class ThemeSwitcher extends BElement {
    extractState({ theme }) {
        return theme;
    }

    view() {
        const iconName = this.state.theme === "dark" ? "🌙" : this.state.theme === "light" ? "☀️" : "🖥️";
        
        return html`
            <button class="theme-toggle" 
                    aria-label="Toggle theme" 
                    @click=${toggleTheme}>
                ${iconName}
            </button>
        `;
    }
}

customElements.define("theme-switcher", ThemeSwitcher);
