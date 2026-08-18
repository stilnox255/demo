# Frontend Design System

This document is the single source of truth for the visual design of this web app.
**When new design decisions are made, update this document to reflect them.**

---

## Design Tokens

All tokens are CSS custom properties on `:root`. **Always use tokens — never hardcode colours or spacing.**

### Colours (dark default)

| Token | Value | Usage |
|-------|-------|-------|
| `--color-bg-gradient` | `linear-gradient(150deg, #0f172a 0%, #1a3050 45%, #0f4c8a 100%)` | `body` background |
| `--color-bg` | `#0f172a` | solid fallback, tab active background |
| `--color-surface` | `rgba(255,255,255,0.07)` | cards, inputs, sidebar background |
| `--color-surface-hover` | `rgba(255,255,255,0.11)` | hover state of surface elements |
| `--color-text` | `rgba(255,255,255,0.92)` | primary text |
| `--color-text-muted` | `rgba(255,255,255,0.50)` | labels, descriptions, footer |
| `--color-border` | `rgba(255,255,255,0.12)` | all borders and dividers |
| `--color-primary` | `#3b82f6` | buttons, links, active indicators, focus rings |
| `--color-primary-dim` | `rgba(59,130,246,0.25)` | active sidebar link background |

### Light theme (`:root[data-theme="light"]`)

| Token | Value |
|-------|-------|
| `--color-bg-gradient` | `linear-gradient(150deg, #c5d8f8 0%, #daeaff 45%, #b8cff5 100%)` |
| `--color-bg` | `#daeaff` |
| `--color-surface` | `rgba(255,255,255,0.55)` |
| `--color-surface-hover` | `rgba(255,255,255,0.75)` |
| `--color-text` | `rgba(10,25,60,0.90)` |
| `--color-text-muted` | `rgba(10,25,60,0.50)` |
| `--color-border` | `rgba(10,25,60,0.12)` |
| `--color-primary` | `#1a56db` |
| `--color-primary-dim` | `rgba(26,86,219,0.15)` |

`data-theme="auto"` mirrors the light values when `prefers-color-scheme: light` is active.

### Spacing & Shape

| Token | Value | Usage |
|-------|-------|-------|
| `--spacing-unit` | `1rem` | base unit — multiply for larger gaps |
| `--radius` | `6px` | standard border-radius |
| `--sidebar-width` | `240px` | fixed sidebar width |

Derived radii:
- **Cards / dialogs:** `calc(var(--radius) * 2.5)` ≈ 15 px
- **Landing feature cards:** `calc(var(--radius) * 3)` ≈ 18 px
- **Pill shape:** `border-radius: 100vw` (badges, status indicators, CTA buttons)
- **Small buttons:** `calc(var(--radius) * 0.75)` ≈ 4.5 px

---

## Typography

- **Font stack:** `system-ui, -apple-system, sans-serif`
- **Line height:** `1.5` (body), `1.15` (hero h1), `1.55–1.65` (landing body text)

| Role | Size |
|------|------|
| App header `h1` | `clamp(1.1rem, 3vw, 1.35rem)` |
| Landing hero `h1` | `clamp(2rem, 5vw, 3.25rem)` |
| Card / section heading `h2` | `1.1rem` |
| Body / table cells | `0.9rem` |
| Labels (uppercase) | `0.8–0.85rem` |
| Small / muted text | `0.78–0.82rem` |
| Footer | `0.8rem` |

Header and card headings: `font-weight: 700`, `letter-spacing: -0.015em` to `-0.02em`. Uppercase labels: `letter-spacing: 0.04–0.05em`.

---

## Global Background Effects

Every page shares two background layers:

1. **Gradient background** — `body` uses `--color-bg-gradient`
2. **Circuit-grid overlay** — `body::before`: semi-transparent 40×40 px grid of lines at `rgba(255,255,255,0.035)`, `position: fixed`, `pointer-events: none`. All content above via `z-index: 1` on `body > *`.

---

## Page Layout

```
┌────────────────────────────────────────────────────────┐
│ <header>  (sticky, z-index 1002, backdrop-blur 20px)   │
├──────────────┬─────────────────────────────────────────┤
│  .sidebar    │  .view                                  │
│  (240px,     │  (flex: 1, padding 2rem × 1.5rem,       │
│  sticky      │   max-width 60rem on ≥1400px)           │
│  desktop /   │                                         │
│  fixed       │                                         │
│  mobile)     │                                         │
├──────────────┴─────────────────────────────────────────┤
│ <footer>  (text-align: end, muted text)                │
└────────────────────────────────────────────────────────┘
```

- `<header>`: `background: rgba(10,20,50,0.65)`, `backdrop-filter: blur(20px)`, `border-block-end: 1px solid var(--color-border)`. App title left, auth box + theme toggle right.
- `<main>`: `display: flex`, fills remaining height.
- `.sidebar`: ≥1024 px — sticky, always visible. <1024 px — fixed, CSS-only hamburger toggle.
- `.view`: router outlet, left-aligned, capped at `60rem` on wide viewports.
- `<footer>`: `background: rgba(10,20,50,0.5)`, small muted text.

---

## Components

Reuse these classes before adding new ones.

### Sidebar Navigation Links

```
border-inline-start: 3px solid transparent   ← inactive
border-inline-start-color: var(--color-primary)  ← .active
background: var(--color-primary-dim)         ← .active
color: rgba(255,255,255,0.75)                ← inactive
color: white                                 ← hover / active
```

Font size: `0.92rem`. Gap between items: `2px`.

### Buttons

| Class | Background | Border | Text | Use |
|-------|-----------|--------|------|-----|
| `.primary-button` | `var(--color-primary)` | none | white | Primary CTA |
| `.secondary-button` | transparent | `rgba(255,255,255,0.4)` | white | Secondary actions in dark contexts |
| `.small-button` | `var(--color-surface)` | `var(--color-border)` | `var(--color-text)` | Inline actions, table rows |
| `.small-button.danger` | hover: `rgba(239,68,68,0.15)` | `rgba(239,68,68,0.3)` | `#f87171` | Destructive inline action |

All buttons: `transition: opacity 0.2s, background 0.2s`, hover `opacity: 0.88`.
Focus: `outline: 2px solid var(--color-primary); outline-offset: 2px`.

### Cards / Sections (`.config-section`, `.status-page`)

```css
background: var(--color-surface);
backdrop-filter: blur(12px);
border: 1px solid var(--color-border);
border-radius: calc(var(--radius) * 2.5);   /* ~15px */
padding: calc(var(--spacing-unit) * 1.75);
margin-block-end: calc(var(--spacing-unit) * 1.5);
```

Section `h2` inside: `font-size: 1.1rem`, `font-weight: 700`, `border-block-end: 1px solid var(--color-border)`.

### Badges

Small pill-shaped labels: `border-radius: 100vw`, `font-size: 0.7rem`, `font-weight: 600`, `text-transform: uppercase`.

| Class | Background | Text |
|-------|-----------|------|
| `.badge-active` / `.badge-success` | `rgba(34,197,94,0.15)` | `#4ade80` |
| `.badge-deleted` | `rgba(239,68,68,0.15)` | `#f87171` |
| `.badge-warning` | `rgba(251,146,60,0.15)` | `#fb923c` |
| `.badge-info` | `rgba(96,165,250,0.15)` | `#60a5fa` |
| `.badge-pending` | `var(--color-surface)` | `var(--color-text-muted)` |

### Status Indicators (`.status-badge`, `.service-status`, `.status-indicator`)

| State | Background | Text | Border |
|-------|-----------|------|--------|
| `up` | `rgba(34,197,94,0.12–0.15)` | `#4ade80` | `rgba(34,197,94,0.25–0.3)` |
| `down` | `rgba(239,68,68,0.12–0.15)` | `#f87171` | `rgba(239,68,68,0.25–0.3)` |
| `loading` / `unknown` | `var(--color-surface)` | `var(--color-text-muted)` | `var(--color-border)` |

### Data Table (`.data-table`)

- Full-width, `border-collapse: collapse`
- `th`: `background: rgba(255,255,255,0.04)`, `font-size: 0.8rem`, uppercase, `color: var(--color-text-muted)`
- `td`/`th` padding: `0.7rem` block × `1rem` inline
- Row separator: `border-block-end: 1px solid var(--color-border)` (last row: none)
- Row hover: `background: var(--color-surface-hover)` — deleted rows: `opacity: 0.45`

### Tab Bar (`.tab-bar` / `.tab-button`)

- Inactive: `color: var(--color-text-muted)`, transparent border
- Active: `color: var(--color-text)`, `background: var(--color-bg)`, `border: 1px solid var(--color-border)`, `border-block-end-color: transparent`

### Forms / Inputs

Labels: `font-weight: 600`, `font-size: 0.8rem`, `text-transform: uppercase`, `letter-spacing: 0.04em`, `color: var(--color-text-muted)`.

```css
/* input */
background: var(--color-surface);
border: 1px solid var(--color-border);
border-radius: var(--radius);
color: var(--color-text);
font: inherit;
/* focus */
border-color: var(--color-primary);
background: var(--color-surface-hover);
outline: none;
```

Upload forms: `<details>` pattern (collapsed by default, `.upload-form`) with the same surface/border style.

### Layout Helpers

- `.action-bar` — flex row for action buttons, `border-block-start: 1px solid var(--color-border)`, `margin-block-start: 1.25rem`
- `.list-controls` — flex row for filters and controls above a list

### Dialog (`.dialog-overlay` / `.dialog`)

- Overlay: `background: rgba(0,0,0,0.65)`, `backdrop-filter: blur(4px)`, `z-index: 2000`
- Dialog box: `background: rgba(15,28,60,0.95)`, `backdrop-filter: blur(20px)`, `border-radius: calc(var(--radius) * 2.5)`, max-width `480px`
- Inputs inside: `rgba(255,255,255,0.06)` / `0.09` on focus, always `color: white`

### Toast Notification (`<toast-container>`)

**File:** `web-app/src/notification/boundary/ToastContainer.js`

Fixed-position notification stack mounted once in `AppShell`. Subscribes to `store.notifications.toasts`.

Dispatch via control functions — never dispatch directly:
```javascript
import { showSuccessToast, showErrorToast, showActionToast, dismissToast } from "../../notification/control/NotificationsControl.js";
showSuccessToast("Component saved");
showErrorToast("Request failed", "Connection refused");
```

State shape: `{ id: number, type: "success" | "error" | "warning", title: string, detail: string | null, action: { label: string, type: string } | null }`

Behaviour: success auto-dismisses after 4 s, error/warning after 8 s, manual dismiss via ✕.

CSS classes: `toast-container` (fixed, bottom-right, z-index 3000), `.toast`, `.toast--success`, `.toast--error`, `.toast__title`, `.toast__detail`, `.toast__close`.
Accessibility: `role="log"`, `aria-live="polite"` on container; `role="alert"` per toast.

#### Action variant

A toast may carry an optional `action: { label, type }` payload field, dispatched via `showActionToast({ type, title, detail, action })`. It renders a primary-coloured `.toast__action` button between the body and the dismiss ✕. Action-bearing toasts **do not auto-close** — they persist until the user clicks the action or dismisses. Handlers are looked up by `action.type` in a small registry populated at startup via `registerToastActionHandler(type, handler)`. Clicking the action invokes the handler and dismisses the toast. First consumer: the session-expired toast (T-06) registers `'login'` → OIDC redirect.

### Empty / Feedback States

| Class | Style |
|-------|-------|
| `.empty-state` | `text-align: center`, `padding-block: 2rem`, `color: var(--color-text-muted)`, `font-size: 0.9rem` |
| `.success-message` | `color: #4ade80`, `font-size: 0.9rem` |
| `.error` | `color: #f87171`, `font-size: 0.9rem` |
| `.auth-message` | `text-align: center`, `padding-block: 3rem`, muted |
| `.status-note` | `font-size: 0.82rem`, muted |

### Pipeline Components (`.pipeline-view`, `.role-lane`, `.stage-tile`)

**Pipeline view** (`.pipeline-view`): section heading + vertical stack of lanes. Heading has bottom border.

**Role lane** (`.role-lane`): glassmorphism card (same surface/border/radius as `.config-section`). Title `1rem bold`, stages in 5-column grid (collapses to 2-col below 700 px via `@container`).

**Stage tile** (`.stage-tile`): smaller card inside a lane. `stage-tile__title` = `0.8rem uppercase muted label`. `stage-tile__count` = `1.5rem bold` primary text. `.stage-tile--loading` dims count to 50% opacity. `.stage-tile__skeleton`: shimmer placeholder (skeleton-shimmer animation). `.stage-tile__recent-list`: compact `<ul>` below the count showing last N items (`0.75rem`, muted, border-top separator). `.stage-tile__recent-item`: flex row id + date. `.stage-tile__error-dot`: small red `●` with `cursor: help` tooltip for per-tile error state (color `--color-error` / `#e05252`).


---

### Thumbnail / Image Placeholders

`.thumbnail-wrapper` (56×56 px, `border-radius: 4px`). Loading: `background: var(--color-border)` + `animation: pulse 1.4s ease-in-out infinite`.

---

## Landing Page

Standalone full-viewport section, does **not** use the app shell layout.

- `.landing-logo`: 72×72 px, `filter: drop-shadow(0 0 16px rgba(0,150,255,0.5))`, pulse animation
- `.landing-hero h1`: `clamp(2rem, 5vw, 3.25rem)`, `letter-spacing: -0.025em`
- `.landing-login-btn`: pill CTA, white background, `color: #0f4c8a`, hover lifts `translateY(-2px)`
- `.landing-feature-card`: glassmorphism, hover `translateY(-3px)` + brighter border

---

## Responsiveness

- **Breakpoints:** `768px` (nav layout), `1024px` (sidebar mode), `1400px` (content max-width)
- Mobile: hamburger toggle (CSS-only), sidebar slides in from left
- `min-block-size: 100dvh` — use `dvh`, not `vh`

---

## Accessibility

- `.skip-link` on every page, visually hidden until focused
- All interactive elements: `:focus-visible` outline (`2px solid var(--color-primary)`)
- `@media (prefers-reduced-motion: reduce)` disables all animations globally
- `accent-color: var(--color-primary)` on checkboxes
- External links: `↗` suffix via `a[target="_blank"]::after`
- Semantic HTML and `aria-*` attributes expected in all components
