# Engineering Guidelines (binding)

## Always-on

No local always-on file in this repo right now. `release-it.md` and
`use-the-platform.md` used to live here as `@`-imports; both moved to the
`book-guidelines` plugin (see below), which injects them as hidden context on every
`SessionStart` via a hook — the same mechanism the `ponytail` plugin uses for its
ruleset. That works across every project once the plugin is installed, not just this
one, so the local copy + `@`-import is gone.

To add a **new** always-on policy that's specific to this project only: drop the file
here and add an `@`-import below. The path is relative to **this file**, not to the
repo root, so write `@my-policy.md` and not `@docs/guidelines/my-policy.md`. A wrong
path is ignored without any error, so verify with `/context` in a fresh session that
the file shows up under *Memory files*.

## Contextual (loaded on demand as skills)

These guidelines are loaded **only when a task falls into their context**, via skills
whose `description:` is the trigger.

Four skill groups don't depend on this project's domain, so they no longer live in
this repo — they ship from the
[`eng-guidelines`](https://github.com/stilnox255/eng-guidelines) Claude Code plugin
marketplace:

- `book-guidelines` — Clean Code, Clean Architecture, PoEAA, HPJP, Modern Software
  Engineering, Philosophy of Software Design, plus PR-review/prototyping/behavioral
  skills, plus the two always-on docs above (hook-injected, not skill-triggered).
- `epic-workflow` — `capture-idea`, `define-epic`, `plan-epic`, `execute-tasks`,
  `github-projects`. Reads its config from **this repo's** `GitHub Project Integration`
  section in `CLAUDE.md`, same as when it lived locally.
- `architecture` — `backend` (Hexagonal Architecture, one class per use case) and
  `frontend` (web-components, BCE). `backend` used to link out to this repo's
  `ADR-14` for its exception-mapper-logging convention; that table was already
  inlined in the skill, the ADR link was only for the rationale, so it was dropped —
  the skill now carries no link into this repo. `ADR-14` itself is unchanged: it
  still records *that* this project adopted the convention and which test enforces
  it, it's just no longer the canonical source of the convention itself.
- `quarkus-tooling` — `quarkus` and `keycloak-administration`, vendored (MIT,
  see `plugins/quarkus-tooling/ATTRIBUTION.md` in that repo) instead of tracked by
  the local skill installer. Same tradeoff `book-guidelines` already made for its
  ciembor-sourced content: no live drift detection, but installed once instead of
  copy-pasted per project.

Install once: `/plugin marketplace add stilnox255/eng-guidelines`, then
`/plugin install book-guidelines@ingo-eng-guidelines`,
`/plugin install epic-workflow@ingo-eng-guidelines`,
`/plugin install architecture@ingo-eng-guidelines`, and/or
`/plugin install quarkus-tooling@ingo-eng-guidelines`. Updates then arrive via
`git pull` on the marketplace, not by re-copying files into this repo.

`quarkus-panache-smells` stays local, alone now: it's tracked by a skill installer
(exact tool unconfirmed — the `skills-lock.json` schema most closely matches the
`skills.re` family, but not byte-for-byte) in `.agents/skills/`, symlinked into
`.claude/skills/`. Its source (`emvnuel/skill.md`) has **no LICENSE file and no
license text anywhere in the repo** — default copyright applies, no redistribution
permission — so it can't be vendored into the public `eng-guidelines` marketplace the
way `quarkus`/`keycloak-administration` were. Recorded provenance:

| Skill | Source | Path |
|---|---|---|
| `quarkus-panache-smells` | `emvnuel/skill.md` | `quarkus-panache/SKILL.md` |

(Also in `skills-lock.json`, with a content hash for drift detection.)

To add a contextual guideline that's specific to **this** project's domain, drop the
file here and create a skill in `.claude/skills/<name>/SKILL.md` that reads it. If
it's generic across projects instead, it belongs in `eng-guidelines`, not here.
