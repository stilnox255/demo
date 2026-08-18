---
name: define-epic
description: Use when the user wants to create a new epic, define a feature, or says things like "neues Epic", "definiere E12", "ich brauche ein Feature für...".
model: opus
---

Define a new Epic as **Product Owner / Architect** — focus on *what* and *why*, not *how*.

**REQUIRED SUB-SKILL:** Use `superpowers:brainstorming` for the design conversation (context exploration, clarifying questions one-at-a-time, 2–3 alternative approaches, incremental approval, spec self-review). Override the default spec location to `docs/epics/E{n}-{kebab-slug}.md`.

**GitHub sync:** Use `github-projects` SKILL for all GraphQL/gh templates and config variables.

## Step 0: Idea promotion (optional)
If this epic promotes a captured idea (user names an idea, mentions "promote idee", or passes a `docs/ideas/*.md` path):
- Read `docs/ideas/{slug}.md` — use its content as seed input for Step 2 brainstorming (problem statement + any direction already recorded save the user from re-typing). Ideas carry no number; the slug is the identifier (see `docs/superpowers/specs/2026-08-14-ideas-without-numbers-design.md`).
- Remember the slug — Step 6 deletes the idea file and its index line in the same commit as the epic artifacts.

## Step 1: Numbering
- **Next epic:** scan `ROADMAP.md` index and `docs/epics/E{n}-*.md` for highest `E{n}`
- **Next ADR:** `ls docs/adr/` for highest `ADR-{nn}`
- Check open milestones: `gh api repos/{REPO}/milestones --jq '.[] | select(.state == "open") | .title'` — flag dependency on incomplete predecessors

## Step 2: Brainstorm (delegated)
Invoke `superpowers:brainstorming`. It handles: alternatives check (simpler approach / extend existing / established pattern / defer), user story, goal, scope in/out, API/UX design, and user approval gates.

**Situational patterns — apply by name when the epic fits:**
- **Shape Up appetite**: fixed time, flexible scope when there's a hard deadline
- **Event Storming**: run before API/UX when the epic spans multiple aggregates or unclear domain boundaries
- **C4 model**: frame API/UX at **Container** (cross-service) or **Component** (single BC) level
- **Bounded Context / Context Mapping** (DDD): when crossing existing BCs, name the mapping pattern (Shared Kernel / Anti-Corruption Layer / Customer-Supplier) instead of inventing glue

When brainstorming writes the spec, save to `docs/epics/E{n}-{kebab-slug}.md` with these sections: **Goal**, **Motivation**, **User Story**, **Success Signal** (how we'll know it worked — one concrete observable: metric, behavior, or user outcome; skip only if the epic is pure infrastructure with no user-facing effect), **Scope** (In / Out), **API / UX**, **Architecture Decisions** (ADR link list), **Scope by component** (plain bullets, no `[ ]` — `plan-epic` does the breakdown).

## Step 3: ADRs
For each non-trivial design choice create `docs/adr/ADR-{nn}-{kebab-slug}.md`:

```markdown
# ADR-{nn}: {Title}
**Status:** Proposed
**Epic:** E{n}
**Reversibility:** low | medium | high  *(low = hard to undo: schema migrations, public API contracts, persisted data formats; high = easy to swap: internal helper choice, local naming)*
**Supersedes:** ADR-XX (if applicable)

## Context
## Decision
## Rationale
## Consequences
```

Reference in epic file: `- [ADR-{nn}: {Title}](../adr/ADR-{nn}-{slug}.md) — one-sentence summary`.

Rules: every ADR must explain *why* (Context + Rationale mandatory); do not mark old ADRs obsolete yet (that happens in `execute-tasks` when the replacement is implemented); present options when multiple valid approaches exist.

## Step 4: Concept Gap Check
Apply the checklist from `.claude/skills/define-epic/gap-check.md` before creating issues.

## Step 5: GitHub — Issue + Milestone + Project
Using `github-projects` SKILL:

1. **Epic issue:**
   ```bash
   gh issue create -R {REPO} \
     --title "E{n}: {Short Goal}" \
     --label "epic,{backend|frontend|ai-service|devops}" \
     --body "..."  # Goal + Motivation + pointer to docs/epics/tasks/E{n}-TASKS.md
   ```
2. **Add to project, Status = Backlog** (github-projects `Add issue to project` + `Set status` with `{STATUS_BACKLOG}`)
3. **Milestone:**
   ```bash
   gh api repos/{REPO}/milestones --method POST \
     -f title="E{n}: {Short Goal}" -f description="{one-line goal}"
   ```

Report Epic issue URL and Milestone URL to the user.

## Step 6: ROADMAP.md index + idea cleanup
Append one line under `## Epics` in numeric order — no body duplication:
```
- [E{n}: {Short Goal}](docs/epics/E{n}-{slug}.md) — one-line summary (Issue #NNN, Milestone)
```

**If Step 0 identified an idea source:**
- Delete `docs/ideas/{slug}.md` (the brain-dump is superseded by the epic). The companion wiki keeps the full text, so nothing is lost.
- Delete the idea's line from `docs/ideas/README.md` (if the index exists).
- Land both deletions in the same commit as the epic file + ADRs + ROADMAP entry. The epic does not reference the idea — it stands on its own.

## Output
- `docs/epics/E{n}-{slug}.md` (one file per epic, structure above)
- `docs/adr/ADR-{nn}-*.md` for each non-trivial decision
- One-line `ROADMAP.md` index entry
- Epic issue + Milestone on GitHub, added to Project V2 with Status = Backlog
- If promoted from an idea (Step 0): the idea file + its index line are deleted in the same commit
- TASKS file is **not** created here — `plan-epic` does that
- All content in English

## Rules
- One goal per epic — related improvements belong in follow-up epics
- Every ADR lives at `docs/adr/ADR-{nn}-{slug}.md`, never inline in epic or ROADMAP
