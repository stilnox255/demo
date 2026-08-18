# Engineering Guidelines (binding)

## Always-on (imported into every session via CLAUDE.md)

Operational binding policy — small, always loaded:

@release-it.md
@use-the-platform.md

## Contextual (loaded on demand as skills)

These guidelines are loaded **only when a task falls into their context**, via skills
whose `description:` is the trigger.

Two skill groups don't depend on this project's domain, so they no longer live in this
repo — they ship from the [`eng-guidelines`](https://github.com/stilnox255/eng-guidelines)
Claude Code plugin marketplace:

- `book-guidelines` — Clean Code, Clean Architecture, PoEAA, HPJP, Modern Software
  Engineering, Philosophy of Software Design, plus PR-review/prototyping/behavioral
  skills.
- `epic-workflow` — `capture-idea`, `define-epic`, `plan-epic`, `execute-tasks`,
  `github-projects`. Reads its config from **this repo's** `GitHub Project Integration`
  section below, same as when it lived locally.

Install once: `/plugin marketplace add stilnox255/eng-guidelines`, then
`/plugin install book-guidelines@ingo-eng-guidelines` and/or
`/plugin install epic-workflow@ingo-eng-guidelines`. Updates then arrive via `git pull`
on the marketplace, not by re-copying files into this repo.

`quarkus`, `quarkus-panache-smells`, and `keycloak-administration` stay separate again:
those were vendored by some skill-installer CLI (exact tool unconfirmed — the
`skills-lock.json` schema most closely matches the `skills.re` family, but not
byte-for-byte) into `.agents/skills/`, symlinked into `.claude/skills/`. Until that's
pinned down, this is the recorded provenance so a re-fetch is possible by hand:

| Skill | Source | Path |
|---|---|---|
| `keycloak-administration` | `dauquangthanh/hanoi-rainbow` | `skills/keycloak-administration/SKILL.md` |
| `quarkus` | `b6k-dev/quarkus-skill` | `skill/quarkus/SKILL.md` |
| `quarkus-panache-smells` | `emvnuel/skill.md` | `quarkus-panache/SKILL.md` |

(Also in `skills-lock.json`, with a content hash per skill for drift detection.)

To add another always-on policy: drop the file here and add an `@`-import under the
Always-on section. The path is relative to **this file**, not to the repo root, so write
`@my-policy.md` and not `@docs/guidelines/my-policy.md`. A wrong path is ignored without
any error, so verify with `/context` in a fresh session that the file shows up under
*Memory files*.

To add a contextual one that's specific to **this** project's domain, drop the file
here and create a skill in `.claude/skills/<name>/SKILL.md` that reads it. If it's
generic across projects instead, it belongs in `eng-guidelines`, not here.