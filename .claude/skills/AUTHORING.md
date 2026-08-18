# Skill Authoring — Principles for This Repo

Read before editing any SKILL.md under `.claude/skills/`. Keeps token cost, team alignment, and discipline consistent.

## 1. Delegate, don't duplicate

Project-specific skills (`define-epic`, `plan-epic`, `execute-tasks`) delegate generic process to `superpowers:*` sub-skills:

| Our skill | Delegates generic flow to |
|---|---|
| `define-epic` | `superpowers:brainstorming` |
| `plan-epic` | `superpowers:writing-plans` |
| `execute-tasks` | `superpowers:executing-plans` / `superpowers:subagent-driven-development` / `superpowers:verification-before-completion` / `superpowers:finishing-a-development-branch` |

**Keep inline** only what's project-specific: `T-XX` IDs, `ROADMAP.md`/`docs/epics/` layout, GitHub Projects V2 sync via `github-projects`, `Closes #NN / Epic: #YY` commit footer, BCE layering, worktree path `.claude/worktrees/worktree-<name>`.

**Do NOT pull `superpowers:test-driven-development`.** TDD is already mandated by `backend/SKILL.md` (§ "Test-Driven Development"). Double-loading wastes tokens and risks conflicting guidance. Same rule applies to any future component-level SKILL.md that encodes a discipline — check first.

## 2. Named patterns over descriptions (but verify)

A pattern name replaces a paragraph **iff** the LLM decodes it unambiguously.

- **Stable + unambiguous → name it:** INVEST, Walking Skeleton / Vertical Slice, Expand-Contract / Parallel Change, Strangler Fig, Rule of Three, Shape Up "appetite", Event Storming, C4 model, Bounded Context, Anti-Corruption Layer, DRY / KISS / YAGNI.
- **Multiple interpretations → describe:** TDD, "clean code," "agile" — the name isn't enough. `superpowers:test-driven-development` is 370 lines for exactly this reason.
- **Needs qualification ("Fowler's X, not Y's X") → write it out.** Naming then costs more than describing.

**Quick test before committing a name:** ask a fresh subagent to produce the shape implied by the name alone, no qualification. If the output matches intent, the name is sufficient.

### Patterns currently live in this repo's skills

| Skill | Patterns |
|---|---|
| `plan-epic` Step 3 | INVEST, Walking Skeleton / Vertical Slice |
| `execute-tasks` breaking-change row | Expand-Contract (Parallel Change), Strangler Fig |
| `define-epic` Step 2 situational | Shape Up appetite, Event Storming, C4 model, Bounded Context / Context Mapping |
| `backend` DRY line | Rule of Three |

Add more only if they materially change output.

## 3. Token-efficiency guardrails

Tokens spent on a skill cost once per load, but skill **descriptions** are injected into every conversation's system prompt — so description tokens multiply.

- **Frontmatter `description:` = Use-when only.** No workflow summary. A workflow summary creates a shortcut Claude may follow *instead of* reading the body (see `superpowers:writing-skills` § "Description = When to Use, NOT What the Skill Does").
- **Don't restate delegated rules.** If `superpowers:writing-plans` already enforces "no placeholders," don't repeat it in `plan-epic`.
- **Shared blocks live once.** The Concept Gap Check lives in `define-epic/gap-check.md` and is referenced from both `define-epic` and `plan-epic` — don't duplicate.
- **Heavy templates → separate files.** If a code template exceeds ~50 lines, move to a sibling file (`adr-template.md`, `task-template.md`) and reference it.
- **`## Output` and `## Rules` sections** tend to restate earlier content. Keep only project-specific rules; drop everything the body/delegated-skills already enforce.

## 4. When to test a skill change

Adapt `superpowers:writing-skills` TDD-for-skills workflow: dispatch a fresh subagent against the current + modified skill on a realistic input. Compare outputs against a checklist. If the slim version loses a load-bearing behavior, back out or add it back.

This is how the current skills were slimmed — see git log on `.claude/skills/*/SKILL.md` for examples.

## 5. When this doc is wrong

Update it. It's a living convention, not scripture. Before changing a principle, check:
- Was it derived from a specific failure mode (e.g. TDD rationalization, horizontal slicing)? Then the new version must handle that case too.
- Does the change affect multiple skills? Update them together.
