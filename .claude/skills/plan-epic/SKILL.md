---
name: plan-epic
description: Use when the user asks to plan an epic, break down tasks, or says things like "plan E12", "brich E12 in Tasks runter", "plan das nächste Epic".
model: opus
---

Break down epic `$ARGUMENTS` into tasks as **Tech Lead**.

**REQUIRED SUB-SKILL:** Use `superpowers:writing-plans` for: task structure with bite-sized steps, "No Placeholders" rule, spec-coverage self-review, type-consistency check. Override: save to `docs/epics/tasks/E{n}-TASKS.md` (not the superpowers default), and use `T-XX` IDs instead of `Task N`.

Always follow `backend` and `frontend` SKILL rules. TDD is already mandated by `backend/SKILL.md` — do not re-import `superpowers:test-driven-development`.

**GitHub sync:** Use `github-projects` SKILL for all templates and config variables.

## Input
- Locate epic via `ROADMAP.md` index (`docs/epics/E{n}-{slug}.md`) and read it fully — goals, ADRs, scope
- If `$ARGUMENTS` empty, ask which epic to plan

## Step 0: Set Epic Status → In Progress
Find Epic issue number from `ROADMAP.md` index line or epic file header. Using `github-projects` SKILL: get item ID (organization query variant — issue may not yet be in project), then set Status = `{STATUS_IN_PROGRESS}`. Best-effort; skip silently if not found.

## Step 1: Prerequisite Check
- Milestone state: `gh api repos/{REPO}/milestones --jq '.[] | .title + " → " + .state'`
- Fallback: check predecessor TASKS files for `[ ]` / `[~]`
- If target depends on incomplete predecessor, warn and ask whether to proceed

## Step 2: Codebase Analysis
Use an Explore agent to find affected files, existing patterns, conflicts with `backend`/`frontend` SKILL rules. Present conflicts to the user before proceeding.

## Step 3: Task Breakdown (delegated)

**Task shape:** Follow **INVEST** criteria (Independent, Negotiable, Valuable, Estimable, Small, Testable). Prefer **vertical slices** — T-01 ships a thin end-to-end **walking skeleton** (stub processor + real boundary + minimal frontend) so the system is demoable after task one. Do **not** serialize Migration → Entity → BC → Resource → Frontend across T-01…T-05 — that leaves nothing runnable until T-05. Horizontal slicing only where one layer must stabilize before all others depend on it (typically: initial schema migration).

Invoke `superpowers:writing-plans` to write the task list. Project-specific overlays:

**Per-task structure** (replacing writing-plans' "Task N" format):
```markdown
## T-XX: {short description} [ ]

**Depends on:** T-YY, T-ZZ (or "none")
**Files:**
- Create: `exact/path/to/File.java`
- Modify: `exact/path/to/Existing.java:123-145`
- Test: `src/test/java/.../FileTest.java`

**Acceptance criteria:**
- [concrete, verifiable conditions]

**Steps** (bite-sized — writing-plans rules):
- [ ] Step 1: Write failing test — {code block}
- [ ] Step 2: Run — `./gradlew test --tests FileTest`, expect FAIL
- [ ] Step 3: Implement minimum — {code block}
- [ ] Step 4: Run — expect PASS
- [ ] Step 5: Commit — plain conventional-commit message, **no `Closes #NN` / `Epic: #YY` footer**. That footer belongs only to the orchestrator's Step 4 task-completion commit in `execute-tasks` (once the Review Gate returns Ready), never to an in-worktree subagent commit — a task can span several Steps 1–5 cycles, and closing the issue on the first one would falsely mark it done before the acceptance criteria are met.
```

- Status markers: `[ ]` planned · `[~]` in progress · `[x]` done · `[!]` blocked
- Plain records/DTOs without logic: note "tested indirectly through T-XX"
- Apply `superpowers:writing-plans` self-review (spec coverage, placeholder scan, type consistency) before presenting

## Step 4: Concept Gap Check
Apply the checklist from `.claude/skills/define-epic/gap-check.md`. Present a gap summary even if none found.

## Step 5: Dependency Graph & Parallelization
ASCII dependency graph + critical path. **File Ownership Matrix** for parallelizable tasks:
```
| File                    | T-01 | T-02 |
|-------------------------|------|------|
| src/.../Entity.java     |  W   |  R   |
```
Two `W` on the same file → cannot run in parallel.

**Breaking Change Detection:** Mark tasks `breaking` when they change API contracts, schema non-additively, or rename/remove public interfaces. Breaking tasks get a PR with review gate.

## Step 6: Create Issues (Two-Pass)

### Pass 1 — Create all issues, collect IDs
```bash
gh issue create -R {REPO} \
  --title "E{n} T-{nn}: {short description}" \
  --body "## Summary
{1-3 sentences}

> **Detailed spec:** see \`docs/epics/tasks/E{n}-TASKS.md\` — section \"T-{nn}\"

### Acceptance Criteria
{abbreviated}

### Dependencies
Depends on: {T-XX or \"none\"}" \
  --milestone "E{n}: {Title}" \
  --label {backend|frontend|ai-service|devops}
```

Fetch IDs (per `github-projects` SKILL template):
```bash
gh api repos/{REPO}/issues/{NR} --jq '{number: .number, intId: .id, nodeId: .node_id}'
```

Build mapping: `T-01 → {number, intId, nodeId}`.

### Pass 2 — Register dependencies + sub-issues
For each dependent task, per `github-projects` SKILL:
1. **Formal dependency** via `Issue dependencies` template (`blocked_by`, uses `intId`)
2. **Append `### Blocked by` tasklist** to issue body (human-readable `#NN` links)
3. **Add as sub-issue** of Epic (`Add sub-issue to epic`, uses `intId`)
4. **Prioritize** in dependency order (`Prioritize sub-issue`)
5. **Add `breaking` label** if applicable

### Add to project, Status = Ready
Per `github-projects` SKILL: `Add issue to project` → `Set status` with `{STATUS_READY}`.

### Iteration assignment
List iterations (`github-projects` SKILL `List available iterations`), present to user, assign chosen iteration to each task item (including Epic) via `Set iteration`.

### Check existing before creating
```bash
gh issue list -R {REPO} --search "E{n} T-{nn}" --json number
```

## Step 7: Annotate TASKS + update Epic body
- Each task heading: `## T-XX: {description} [ ] (#NN)`
- Clean up Epic issue body if still has placeholder tasklist:
  ```bash
  gh issue edit {EPIC_NR} -R {REPO} --body "## Goal
  {goal}

  ## Scope
  See \`docs/epics/tasks/E{n}-TASKS.md\` for full task breakdown."
  ```

## Step 8: Commit & Push
```bash
git add docs/epics/tasks/E{n}-TASKS.md TASKS.md docs/epics/E{n}-*.md ROADMAP.md
git commit -m "chore(docs): plan E{n} — add TASKS file and index entry"
git push
```

## Output
- `docs/epics/tasks/E{n}-TASKS.md` with T-XX sections + bite-sized steps
- `TASKS.md` root index: one line for the new epic
- Epic file links to TASKS file
- Issue URLs reported to user

## Rules
- **Ask when multiple valid engineering approaches exist** (fan-out mechanism, queue vs. in-process, library choice) — do not pick silently. Non-obvious choices become ADRs.
- If `gh` commands fail, log and continue — TASKS file is source of truth
- All content in English
