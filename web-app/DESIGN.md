# Design

The single source of truth for color, spacing and radius is the `:root` token
block in `src/style.css` — read it there, don't duplicate the values here.
Light, dark and `auto` are the same markup with different `--color-*` values,
switched via `data-theme` on `<html>`. A new view never hardcodes a color or a
bare `rem`/`px` spacing value — only a `var(--color-*)` / `calc(var(--spacing-unit) * n)`
— or it goes stale the moment a token changes.

## Reuse before writing new CSS

A new view matches the existing ones by reusing these, not by reinventing them:

| Need | Reach for |
|---|---|
| A card / grouped section | `.config-section` |
| A button | `.primary-button` (one per view), `.secondary-button`, `.small-button` for inline row actions |
| A tabular list with paging | `<data-table>` + `<pagination-controls>` (`src/shared/boundary/`) |
| "Nothing to show" | `.empty-state` |
| A row of trailing actions (save/cancel, delete) | `.action-bar` |
| A modal | `.dialog-overlay` / `.dialog` |
| A status label | `.status-badge`, `.badge-success` / `.badge-warning` / `.badge-info` |

## Layout rhythm

`.view` caps content width and owns the page padding — a feature view does not
add its own outer padding.

## Structure and feedback

Component layering (`boundary` / `control` / `entity`) and the toast
conventions are already binding — see `ADR-28` and the "Notifications" section
in `CLAUDE.md`. This file is about look, not structure.
