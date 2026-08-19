# ADR-46: Frontend i18n via Bundled Flat Catalogues

**Status:** Accepted
**Reversibility:** medium — the lookup call sites are mechanical to migrate, the
catalogue format is not.

## Context

The frontend shipped hardcoded English, `lang="en"` fixed in the HTML, and no `Intl`
usage anywhere. Any project grown from this starter that needs a second language pays
for the retrofit across every boundary component, which is the expensive moment to
discover it.

A working implementation of the same problem existed in a sibling project
(`bfof-pwa`) on an identical stack — `BElement`, Redux Toolkit, lit-html, the same
three runtime dependencies. So the work started by reading that implementation rather
than by designing one.

Reading it produced three findings that shaped this decision more than the mechanism
itself did:

1. Its catalogues are fetched at runtime from `public/`. The first paint therefore
   renders raw keys until the fetch resolves.
2. Its locale slice starts at `"de"` and its detection guards on a whitelist that
   `"de"` satisfies, so the `navigator.language` branch is unreachable. A French
   visitor gets German.
3. It never updates `<html lang>`, so a screen reader applies German pronunciation
   to French text.

All three are consequences of decisions that looked free at the time. Copying the
module verbatim would have inherited them.

## Decision

**Hand-rolled, no dependency.** A 7-line `t(key, params)` over a flat object, the
lookup adopted unchanged from `bfof-pwa`. Flat keys with literal dots, `{name}`
placeholders.

**Catalogues are bundled ES modules, not fetched.** `src/i18n/entity/{en,de}.js`,
imported by `catalogues.js`. This is the deliberate deviation from the source
implementation: it makes `initI18n()` synchronous, so it runs alongside `initTheme()`
before the first render and no frame paints untranslated. It also removes the fetch
error path — a failed catalogue load has no degraded state to design, because there
is no load.

**`CATALOGUES` is the only place a locale is declared.** The supported list and the
switcher's cycle order both derive from `Object.keys`. The source implementation had
four parallel constants plus two inline arrays.

**The slice starts at `locale: null`.** A persisted choice has to be distinguishable
from never having chosen, or detection cannot reach the browser.

**No reactivity of its own.** `t()` is a plain read of a module variable. Components
retranslate because `localeChanged` swaps the catalogue and *then* dispatches, and
`BElement` subscribes every mounted component to the whole store. Nothing subscribes
to the locale for translation purposes, and the ordering of those two statements is
load-bearing.

**Toast fields are catalogue keys.** A toast lives in the store, so text resolved at
dispatch time would keep the language it was raised in. `ToastContainer` runs every
field through `t()` instead. The key-miss fallback is what makes this cheap:
server-supplied `problem+json` text misses the catalogue and comes back out unchanged,
so the same call handles both kinds of text without a flag to distinguish them.

## Alternatives

**i18next.** Plural rules, ICU messages, an extraction ecosystem, `<Trans>`-style
rich interpolation. Rejected: a fourth runtime dependency for a catalogue of ~90
strings, none of which depends on a count. Nothing in the app currently needs what it
adds.

**lit-localize.** Fits lit-html and gives compile-time key checking. Rejected: it
introduces a build step and an XLIFF workflow, and its `msg()` needs a locale-change
mechanism we would still be writing.

**Runtime-fetched JSON, as in the source implementation.** Lets a translator change a
string without a rebuild. Rejected: the raw-keys-on-first-paint bug is structural, and
nobody is editing catalogues without a deploy here.

**`Intl.PluralRules` for plurals.** Deferred, not rejected. There is currently no
count-dependent string. Adding it later is a helper, not a migration.

## Consequences

- **No plural rules.** The first `{count} items` string has to add them.
- **No extraction tooling and no key checking.** A key present in one catalogue and
  missing from the other renders as the key. Parity is checked by reading, and the
  fallback is deliberately visible rather than empty.
- **`t()` returns a string, not a `TemplateResult`.** A translated sentence cannot
  contain markup or a nested component. `ApiDocumentation` is already shaped around
  this: the sentence ends before the link rather than wrapping it.
- **Server text stays English.** `problem+json` titles and Bean Validation messages
  come from a backend with no `Accept-Language` handling. This is the one user-visible
  text a locale switch does not reach, and closing it is a backend decision.
- **`public/callback.html` and `callback.js` stay English.** They cannot import a
  bundled module under `script-src 'self'`. A transient page plus one `alert` on a
  failure path.
- **The pre-boot shell stays English** — the meta description and the skip link in
  `index.html`. `document.title` and `lang` are set from `initI18n()`.
- **Playwright pins `locale: 'en-US'`.** Without it the suite's text assertions would
  depend on the host's language.
- The English catalogue's values are byte-identical to the strings they replaced,
  including uppercase status values, so the existing specs kept passing unchanged.

## Related

- ADR-28 — the layering the module follows: action creator in `control`, reducer in
  `entity`, store injected via `setStore`
- ADR-29, ADR-30, ADR-31 — the toast mechanism whose payload now carries keys
- ADR-08 — the problem-details payload that passes through the lookup untranslated
- ADR-42 — the CSP that rules out a CDN-hosted catalogue
