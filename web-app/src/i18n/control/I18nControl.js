import { createAction } from "@reduxjs/toolkit";
import { CATALOGUES, FALLBACK_LOCALE, SUPPORTED_LOCALES } from "../entity/catalogues.js";

export const localeChangedAction = createAction("localeChanged");

let storeInstance = null;
let activeLocale = FALLBACK_LOCALE;
let translations = CATALOGUES[FALLBACK_LOCALE];

export const setStore = (store) => {
    storeInstance = store;
};

/**
 * Look up a translation. A miss returns the key itself, which is load-bearing in
 * two places: `ToastContainer` runs every toast field through `t()`, so
 * server-supplied `problem+json` text passes through unchanged, and a key added
 * to one catalogue but not the other stays visible instead of rendering empty.
 *
 * `{placeholder}` tokens are replaced from `params`; an unknown token is left
 * literal so a typo shows up rather than silently vanishing.
 */
export function t(key, params) {
    const template = translations[key] || key;
    if (!params) return template;
    return template.replace(/\{(\w+)\}/g, (match, name) =>
        Object.prototype.hasOwnProperty.call(params, name) ? params[name] : match
    );
}

export const currentLocale = () => activeLocale;

/**
 * Swap the catalogue, then dispatch. The order matters: `t()` is a plain read of
 * a module-level variable with no reactivity of its own, so the catalogue has to
 * be in place before the dispatch that re-renders every mounted component.
 */
export const localeChanged = (locale) => {
    const next = SUPPORTED_LOCALES.includes(locale) ? locale : FALLBACK_LOCALE;
    activeLocale = next;
    translations = CATALOGUES[next];
    document.documentElement.setAttribute("lang", next);
    document.title = t("app.title");
    if (storeInstance) {
        storeInstance.dispatch(localeChangedAction(next));
    }
};

/**
 * Detection order: the rehydrated choice, then the browser's language, then the
 * fallback. The slice starts at `locale: null` precisely so the persisted value
 * is distinguishable from "never chosen" — an initial state of `"en"` would
 * satisfy the first branch on a first-ever visit and make browser detection
 * dead code.
 */
export const initI18n = () => {
    localeChanged(detectLocale());
};

const detectLocale = () => {
    const persisted = storeInstance?.getState().i18n?.locale;
    if (persisted) {
        return persisted;
    }
    const browserLanguage = navigator.language?.slice(0, 2);
    return SUPPORTED_LOCALES.includes(browserLanguage) ? browserLanguage : FALLBACK_LOCALE;
};

export const toggleLocale = () => {
    if (!storeInstance) return;
    localeChanged(nextLocale());
};

export const nextLocale = () => {
    const index = SUPPORTED_LOCALES.indexOf(activeLocale);
    return SUPPORTED_LOCALES[(index + 1) % SUPPORTED_LOCALES.length];
};

let numberFormat = null;
let numberFormatLocale = null;

/**
 * Locale-aware integer formatting — thousands separators differ between en and
 * de. Memoized because the pagination line reformats on every render and
 * constructing an `Intl.NumberFormat` is the expensive part.
 */
export const formatNumber = (value) => {
    if (numberFormatLocale !== activeLocale) {
        numberFormat = new Intl.NumberFormat(activeLocale);
        numberFormatLocale = activeLocale;
    }
    return numberFormat.format(value);
};

const displayNames = {};

/** The language's own name, e.g. `de` → "Deutsch". Used for the switcher's label. */
export const languageName = (locale) => {
    if (!displayNames[locale]) {
        displayNames[locale] = new Intl.DisplayNames([locale], { type: "language" });
    }
    return displayNames[locale].of(locale);
};
