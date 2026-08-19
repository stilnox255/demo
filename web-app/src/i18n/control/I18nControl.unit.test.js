import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { CATALOGUES, FALLBACK_LOCALE, SUPPORTED_LOCALES } from '../entity/catalogues.js';
import {
    setStore,
    t,
    localeChanged,
    localeChangedAction,
    initI18n,
    toggleLocale,
    currentLocale,
    nextLocale,
    formatNumber
} from './I18nControl.js';

const storeWith = (locale, dispatched) => ({
    getState: () => ({ i18n: { locale } }),
    dispatch: (action) => dispatched.push(action),
});

// There is no extraction tooling and no compile-time key checking (ADR-46), so a
// key added to one catalogue and forgotten in the other would silently render as
// the key. This is the check that stands in for that tooling.
describe('catalogue parity', () => {
    const reference = Object.keys(CATALOGUES[FALLBACK_LOCALE]).sort();

    it.each(SUPPORTED_LOCALES.filter(locale => locale !== FALLBACK_LOCALE))(
        '%s has exactly the keys of the fallback catalogue',
        (locale) => {
            expect(Object.keys(CATALOGUES[locale]).sort()).toEqual(reference);
        }
    );

    it.each(SUPPORTED_LOCALES)('%s has no empty value', (locale) => {
        const empty = Object.entries(CATALOGUES[locale])
            .filter(([, value]) => typeof value !== 'string' || value.trim() === '')
            .map(([key]) => key);
        expect(empty).toEqual([]);
    });
});

describe('t', () => {
    let dispatched;

    beforeEach(() => {
        dispatched = [];
        setStore(storeWith('en', dispatched));
        localeChanged('en');
    });

    it('returns the key verbatim when there is no entry for it', () => {
        // Load-bearing: ToastContainer runs server-supplied problem+json text
        // through t(), and it has to come out unchanged.
        expect(t('Item not found')).toBe('Item not found');
        expect(t('no.such.key')).toBe('no.such.key');
    });

    it('substitutes placeholders from params', () => {
        expect(t('pagination.range', { start: 1, end: 25, total: 60, page: 1, totalPages: 3 }))
            .toBe('1–25 of 60 (page 1/3)');
    });

    it('leaves an unknown placeholder literal so the typo is visible', () => {
        expect(t('pagination.range', { start: 1 })).toContain('{end}');
    });

    it('ignores params when the key misses', () => {
        expect(t('no.such.key', { a: 1 })).toBe('no.such.key');
    });
});

describe('localeChanged', () => {
    let dispatched;

    beforeEach(() => {
        dispatched = [];
        setStore(storeWith('en', dispatched));
        localeChanged('en');
        dispatched.length = 0;
    });

    it('swaps the catalogue', () => {
        expect(t('demo.delete')).toBe('Delete');
        localeChanged('de');
        expect(t('demo.delete')).toBe('Löschen');
    });

    it('sets the document language so assistive tech reads the right one', () => {
        localeChanged('de');
        expect(document.documentElement.getAttribute('lang')).toBe('de');
    });

    it('translates the document title', () => {
        localeChanged('de');
        expect(document.title).toBe('Starter Admin');
    });

    it('dispatches the change, which is what re-renders every component', () => {
        localeChanged('de');
        expect(dispatched).toHaveLength(1);
        expect(dispatched[0].type).toBe(localeChangedAction.type);
        expect(dispatched[0].payload).toBe('de');
    });

    it('falls back for an unsupported locale rather than emptying the catalogue', () => {
        localeChanged('fr');
        expect(currentLocale()).toBe('en');
        expect(t('demo.delete')).toBe('Delete');
    });
});

describe('initI18n detection', () => {
    let dispatched;
    let languageSpy;

    beforeEach(() => {
        dispatched = [];
    });

    afterEach(() => {
        languageSpy?.mockRestore();
        languageSpy = null;
    });

    const withBrowserLanguage = (value) => {
        languageSpy = vi.spyOn(navigator, 'language', 'get').mockReturnValue(value);
    };

    it('prefers the rehydrated choice over the browser', () => {
        withBrowserLanguage('en-GB');
        setStore(storeWith('de', dispatched));
        initI18n();
        expect(currentLocale()).toBe('de');
    });

    it('uses the browser language when nothing is persisted', () => {
        // The regression this guards: with an initial state of "en" instead of
        // null, the persisted branch always wins and this path is unreachable.
        withBrowserLanguage('de-AT');
        setStore(storeWith(null, dispatched));
        initI18n();
        expect(currentLocale()).toBe('de');
    });

    it('falls back to en for a language with no catalogue', () => {
        withBrowserLanguage('fr-FR');
        setStore(storeWith(null, dispatched));
        initI18n();
        expect(currentLocale()).toBe('en');
    });
});

describe('toggleLocale', () => {
    let dispatched;

    beforeEach(() => {
        dispatched = [];
        setStore(storeWith('en', dispatched));
        localeChanged('en');
    });

    it('cycles through the supported locales', () => {
        expect(nextLocale()).toBe('de');
        toggleLocale();
        expect(currentLocale()).toBe('de');
        toggleLocale();
        expect(currentLocale()).toBe('en');
    });
});

describe('formatNumber', () => {
    let dispatched;

    beforeEach(() => {
        dispatched = [];
        setStore(storeWith('en', dispatched));
    });

    it('uses the separator of the active locale', () => {
        localeChanged('en');
        expect(formatNumber(1234)).toBe('1,234');
        localeChanged('de');
        expect(formatNumber(1234)).toBe('1.234');
    });
});
