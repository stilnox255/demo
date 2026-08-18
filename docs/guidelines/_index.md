# Engineering Guidelines (binding)

## Always-on (imported into every session via CLAUDE.md)

Operational binding policy — small, always loaded:

@release-it.md
@use-the-platform.md

## Contextual (loaded on demand as skills)

These guidelines live in this folder but are loaded **only when a task falls into
their context**, via skills in `.claude/skills/`. The skill's `description:` is the
trigger; its body reads the file below.

| Guideline file | Skill (trigger) |
|---|---|
| `a-philosophy-of-software-design.md` | `philosophy-of-software-design` — module/interface/abstraction design |
| `clean-architecture.md` | `clean-architecture` — layering, boundaries, dependency direction |
| `clean-code.md` | `clean-code` — naming, functions, readability at statement level |
| `modern-software-engineering.md` | `modern-software-engineering` — empirical approach, small steps, feedback, testability/deployability as design drivers |
| `patterns-of-enterprise-application-architecture.md` | `enterprise-application-patterns` — persistence/domain/ORM patterns |
| `high-performance-java-persistence.md` | `high-performance-java-persistence` — Hibernate/JPA fetching, N+1, locking, batching |

To add another always-on policy: drop the file here and add an `@`-import under the
Always-on section. The path is relative to **this file**, not to the repo root, so write
`@my-policy.md` and not `@docs/guidelines/my-policy.md`. A wrong path is ignored without
any error, so verify with `/context` in a fresh session that the file shows up under
*Memory files*.

To add a contextual one: drop the file here and create a skill in
`.claude/skills/<name>/SKILL.md` that reads it.