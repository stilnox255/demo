import { en } from "./en.js";
import { de } from "./de.js";

/**
 * The single source of truth for which locales exist. Everything else derives
 * from this object: the supported list, the switcher's cycle order, the
 * fallback. Adding a locale is one import plus one entry here.
 *
 * Catalogues are imported rather than fetched. That keeps `initI18n()`
 * synchronous, so the first paint is already translated — a runtime fetch
 * renders raw keys until it resolves.
 */
export const CATALOGUES = { en, de };

export const SUPPORTED_LOCALES = Object.keys(CATALOGUES);

export const FALLBACK_LOCALE = "en";
