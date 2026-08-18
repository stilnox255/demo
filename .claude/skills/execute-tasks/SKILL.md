---
name: execute-tasks
description: Use when the user asks to implement tasks, work through a milestone, or says things like "führe die Tasks aus", "arbeite TASKS.md ab", "implementiere E22".
model: opus
---

Execute planned (`[ ]`) tasks from `docs/epics/tasks/E{n}-TASKS.md`.

## Overview

Orchestrator (Opus, this session) reads TASKS.md, dispatches every implementation task to a subagent in an isolated worktree, reviews the returned diff, and owns all state (TASKS.md, SESSION.md, GitHub). The orchestrator never writes code itself — that's the Iron Law below. Continuous mode loops task-to-task without pausing for confirmation.

## Iron Law

**The orchestrator never writes code.** Every implementation task — even a single one-line rename — is delegated to a subagent in a worktree. Opus orchestrates: kickoff, briefing, review, state (TASKS/SESSION/GitHub), commit, push. Sonnet/Haiku execute.

**Violating the letter of this rule is violating the spirit.** No "this one is trivial." No "subagent overhead isn't worth it." No "I already have the file loaded." Delegate or stop.

### Why

- Token economics: orchestrator context is the scarcest resource. Code-write loops (read → edit → verify → fix) burn it fastest. Subagents start cold, finish, return a short report.
- Consistency: same dispatch flow for 1 task and N tasks means no special-case bugs in state handling.
- Auditability: every code change traces back to a discrete subagent run with a fixed brief.

### Red Flags — STOP if you think any of these

| Thought | Reality |
|---|---|
| "Just one rename, faster to Edit myself" | Token cost shows up later as truncated context. Dispatch. |
| "Subagent overhead exceeds the work" | Overhead is paid in tokens you don't have. Dispatch. |
| "I already loaded the file" | Subagent re-reads in its own context, not yours. Dispatch. |
| "Skill's sequential path lets me code" | This version forbids it. Re-read Iron Law. Dispatch. |
| "It's only two Edit calls" | Two Edit calls still count as code-writing. Dispatch. |
| "Worktree setup for one task is wasteful" | Always-worktree is the rule. Dispatch. |

All of these mean: dispatch a subagent. No exceptions.

## Required Sub-Skills

- **All execution** → `superpowers:subagent-driven-development` + `superpowers:using-git-worktrees`
- **Plan review / critical reading before dispatch** → `superpowers:executing-plans` (orchestrator uses its review/TodoWrite loop, not its code-execution loop)
- **Per-task review gate** → `superpowers:requesting-code-review`, overriding its generic template with `skills/execute-tasks/code-reviewer.md` (project-aware). See Step 3.5.
- **On completion** → `superpowers:finishing-a-development-branch`

Always follow `backend` and `frontend` SKILL rules. TDD is mandated by `backend/SKILL.md`. Brief subagents on TDD; do not re-import `superpowers:test-driven-development` here.

**Conditional REQUIRED sub-skills — by touched files, not advisory:**

| Task touches | Subagent MUST invoke |
|---|---|
| Panache entity/repository, `adapter.out.persistence.*` | `quarkus-panache-smells` |
| Any `adapter.in.*` or `adapter.out.*` (ports/adapters generally) | `quarkus` |

Put the matching requirement in the dispatch briefing (Step 3) as an explicit instruction, not just a mention — same enforcement level as `requesting-code-review` above.

**GitHub sync:** Use `github-projects` SKILL for all templates and config variables.

## Test Scope Policy (overrides generic TDD guidance for this repo)

`superpowers:test-driven-development`'s Verify GREEN step says "other tests
still pass" without scoping. In this repo that boots `@QuarkusTest` + Docker
Compose devservices (Postgres, Keycloak, AI service, RustFS) — full runs on
every mini-edit burn wall-clock and usage budget for nothing, since CI
(`ci.yml` → `./gradlew check`) already runs the complete suite on every push
to main. Brief every subagent with the scope below, not the generic
instruction.

Two Gradle `Test` tasks, split by JUnit5 tag (`@QuarkusTest` carries
`@Tag("io.quarkus.test.junit.QuarkusTest")` itself — no file moved, no
custom tagging):
- `test` — plain unit tests (Domain/Use Case tier), no Quarkus boot, default heap.
- `quarkusRuntimeTest` — everything `@QuarkusTest`-annotated (Inbound Adapter IT,
  Persistence `*DbTest`, System `*IT`), own JVM with `maxHeapSize=2g` and
  `forkEvery=20` so repeated context-restart memory doesn't accumulate into
  an OOM mid-run. `check`/`build` run both; neither task alone is "the full
  suite."

| Stage | What runs | Command shape |
|---|---|---|
| Inner TDD loop (subagent RED/GREEN, every edit) | Only the test class(es) for the code under change — pick the task by whether that class is `@QuarkusTest` | `./gradlew test --tests "de.ingoschindler.<bc>.<Class>Test"` or `./gradlew quarkusRuntimeTest --tests "de.ingoschindler.<bc>.<Class>IT"` |
| Per-task gate, once, before reabsorb (Step 5) | BC-scoped tests + ArchUnit suite in **both** tasks — no more, no less | `./gradlew test --tests "de.ingoschindler.<bc>.*" --tests "de.ingoschindler.architecture.*"` **and** `./gradlew quarkusRuntimeTest --tests "de.ingoschindler.<bc>.*"` |
| Immediately before `git push` (Step 4.4), at most once, orchestrator only | Full suite — optional local safety net, not mandatory | `./gradlew test quarkusRuntimeTest` |
| Epic close-out (Step 6) | Compile sanity only, not `test`/`quarkusRuntimeTest` | `./gradlew compileJava compileTestJava` |

**Closing the loophole:** "let me just double-check nothing broke" is the
habit this table forbids. A subagent never runs the pre-push row — that row
belongs to the orchestrator alone, at most once, right before that task's
`git push`. If the scoped rows above already went green and no further code
changed, re-running the full suite finds nothing and only risks colliding
with another worktree's own `quarkusRuntimeTest` run. Running bare `./gradlew
test` and calling it "the suite" is now silently wrong — it skips every
`@QuarkusTest` class. Always pair it with `quarkusRuntimeTest` when the intent
is "everything."

### Pre-Flight: spotlessApply + checkstyle (before any build/compile/test command)

`spotlessCheck` and `checkstyleMain`/`checkstyleTest` both already run on
`check`/`build` (see `build.gradle`) and CI (`ci.yml` → `./gradlew check`)
runs `check` on every push to main — so a formatting-only failure surfacing
first at CI is redundant work nobody needed. Run `./gradlew spotlessApply`
(auto-fix, idempotent, no test/compile step) immediately before **every**
command in the table above and before the pre-push row — cheap, and it
prevents a scoped test run failing (or a review-gate diff carrying noise)
over formatting alone.

Before the **per-task gate** run only (Step 3.5, once, not the inner TDD
loop), also run `./gradlew checkstyleMain checkstyleTest` first. It's a
static check with no test boot, so it's near-free before the heavier
`test`/`quarkusRuntimeTest` gate run, and catches semantic lint violations
(unused imports past what Spotless removes, style-rule violations) before
burning a full scoped test cycle on code that would fail `check` anyway.

Order for the gate run: `spotlessApply` → `checkstyleMain checkstyleTest`
→ the scoped `test`/`quarkusRuntimeTest` command. Do not widen either
task's scope beyond default (project-wide) — both are fast enough
unscoped and scoping them adds no value.

### Concurrency Guardrail

**Scope: the `quarkusRuntimeTest` task** (Inbound
Adapter IT, Persistence `*DbTest`, System `*IT` — everything `@QuarkusTest`-
tagged). Never run two of these at once. The `test` task (plain JUnit/
Mockito, no Quarkus runtime — see `backend/SKILL.md` Testing Strategy) boots
no compose stack and is exempt — run it freely in parallel across worktrees.
For `quarkusRuntimeTest`: concurrent runs collide on container/network names
(the actual mechanism behind "port conflicts, host gets slower" symptoms),
not just ports. When parallel task dispatch (Step 3) puts two subagents in
worktrees at once and both touch adapter/persistence tests, serialize just
that test execution — code-writing stays parallel. Never background a
`quarkusRuntimeTest` run and start another before it exits.

#### Diagnose-before-retry on `@QuarkusTest` failure

A scoped test run can fail two different ways — a **real test failure**
(assertion, business logic, compile error) or an **infra failure** (Docker/
Testcontainers couldn't stand up the container the test needs). Blind
full-suite retries are forbidden for both; treat them differently.

**Classify — read the stack trace, don't guess.**

| Signal in gradle output | Classification | Response |
|---|---|---|
| Root cause is `de.ingoschindler.*`, a JUnit `AssertionFailedError`, or a Mockito verification failure | Real test failure | Fix the code. No diagnostics, no retry-as-is. |
| Root cause is `org.testcontainers.containers.ContainerLaunchException`, `com.github.dockerjava.api.exception.ConflictException` / `InternalServerErrorException`, or a message containing `port is already allocated`, `Bind for 0.0.0.0:<port> failed`, `Conflict. The container name ... is already in use`, or a wait-strategy timeout during startup (`Timed out waiting for container port to open`, connection-refused before any `@Test` method ran) | Infra failure | Run diagnostics below before any retry. |

**Diagnose — read-only, before touching anything:**
```bash
docker ps -a --filter "label=io.quarkus.devservice.launch-mode=TEST" \
  --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}\t{{.Names}}"
```
Quarkus Dev Services labels every container it starts with
`io.quarkus.devservice.launch-mode` (`TEST` for `test`/`quarkusRuntimeTest`,
`DEV` for `quarkusDev` — verified live against this repo's pinned Quarkus
3.36.3; filtering on `launch-mode=TEST` specifically also keeps this from
ever touching a developer's own running `quarkusDev` session). Rows in
`Exited`/`Created` state are orphans from a killed prior run — the only
remediation candidates. If the failure named a specific port, cross-check
with `docker ps -a --filter "publish=<port>"` to see what actually holds
it. If `docker ps` itself errors (daemon unreachable), stop and report
immediately — that's outside a subagent's authority.

**Remediate — only orphans, never anything running:**
```bash
docker ps -a --filter "label=io.quarkus.devservice.launch-mode=TEST" --filter "status=exited" -q \
  | xargs -r docker rm
```
This removes only **stopped** Dev Services containers — it can never touch
a container that's `Up`, so it's safe even when another project's
containers share the host. If the colliding port or name is held by a
**running** container (this repo's or an unrelated project's), do not stop
or remove it — that's not an orphan, it's a real conflict outside this
task's authority. Stop and report instead of improvising.

**Retry — same scope, never wider:** re-run the exact `--tests` command
from the Test Scope Policy table you already had. Remediating an orphan
justifies re-running that *same* scoped command once — it never justifies
widening scope, and a real test failure never justifies re-running
anything wider than the class you're already editing.

**Hard cap — 2 diagnose-and-retry cycles per scoped test target, then
stop.** Do not diagnose a third time, do not widen scope, do not fall back
to the full suite "just to see." Report to the orchestrator: the command
run, the classification and evidence for each of the 2 attempts, and the
last `docker ps -a` output. This is a blocker for the orchestrator/user to
resolve, not a retry-until-luck problem.

## Session State

Maintain `.claude/SESSION.md` on **main** (never in worktrees). A `SessionStart` hook surfaces it on every new conversation.

Update SESSION.md at: task kickoff (Step 1b), worktree dispatch / reabsorb, phase transition (RED → GREEN → REFACTOR → DONE), and before any pause (user interrupt, blocker, context pressure).

Format — ≤15 lines, terse:
- **Epic / Task:** `E{n}` / `T-XX` + short title (#issue)
- **Phase:** RED | GREEN | REFACTOR | TASK-DONE | EPIC-DONE | BLOCKED | IDLE-AWAITING-USER
  - `TASK-DONE` = one task complete, more `[ ]` remain → MUST loop to next, NOT a pause point
  - `EPIC-DONE` = all `[ ]` consumed → triggers Step 6 close-out
  - `IDLE-AWAITING-USER` = only after halt protocol or between distinct user invocations. Never between tasks of a running epic.
- **Branch:** `main` or `worktree-<name>` (+ path)
- **Last commit:** `{sha}` — subject
- **In-flight:** one-line note of what was just in progress
- **Next step:** the immediate next action to resume
- **Updated:** ISO date

**On context pressure** (responses slowing, limit warning, long tool outputs stacking): stop, reabsorb any in-flight subagent work, commit any `[~]` state on main, update SESSION.md with accurate `In-flight` + `Next step`, tell the user to start a fresh session.

## Input
- Epic from `$ARGUMENTS` (e.g. "E22") — ask if unspecified
- Scan the TASKS file for `[ ]` (planned) or `[~]` (in progress)
- `$ARGUMENTS` with task ID (e.g. "T-05") → focus on that task only, then stop
- `$ARGUMENTS` without task ID → **continuous mode**: process ALL `[ ]` tasks sequentially. Loop until no `[ ]` remain OR halt condition fires. Do NOT pause between tasks. Do NOT ask user to invoke `/execute-tasks resume` between tasks of the same run.
- `[!]` blocked → skip with note

## Step 1: Preparation
- Read `.claude/SESSION.md` first — resume in-flight work if any
- Read the TASKS file fully — dependencies, acceptance criteria, tests
- Identify independent tasks for parallelization (≥2 independent = parallel dispatch; otherwise single dispatch)
- **Do not load `backend`/`frontend` SKILL into orchestrator context** — subagents load what they need

## Step 1b: Task Kickoff (orchestrator-only — never delegate)
For each task you are about to start, run these three steps in order on **main** *before* any subagent dispatch:
1. Flip TASKS.md `[ ]` → `[~]`, commit + push
2. **GitHub status flip — required, not best-effort.** Using `github-projects` SKILL: get item ID (repository query variant), set Status = `{STATUS_IN_PROGRESS}`. If the call fails, **stop and surface the error** — do not proceed with a lying board
3. Update `.claude/SESSION.md` (commit + push on main)

Only the orchestrator moves issue state and SESSION.md. Subagents only execute code in worktrees.

## Step 2: Conflict Check
- Do tasks require patterns forbidden by `backend`/`frontend`/`CLAUDE.md`/`src/CLAUDE.md`?
- If conflicts exist, **stop and present** to user — do not silently resolve

## Step 3: Execute (always via subagent dispatch)

**Single task → one subagent in one worktree. Parallel tasks → N subagents in N worktrees. Same flow, different N.**

### Per-dispatch protocol — `superpowers:subagent-driven-development`

1. **Worktree** at `.claude/worktrees/worktree-<task-or-group-name>` (project convention — NOT `../`).
2. **Briefing — focused, not exhaustive.** Include only:
   - The TASKS.md *section* for the task (not the full file)
   - Acceptance criteria + tests required
   - The *specific* BCE layer rule(s) that apply (e.g. "JAX-RS Resource rules from `backend/SKILL.md`" — not the whole SKILL). Let the subagent load `backend`/`frontend` itself if it needs more.
   - **Conditional required skill** — if the task touches a Panache entity/repository or `adapter.out.persistence.*`, explicit instruction: "invoke `quarkus-panache-smells` skill before finishing." If it touches `adapter.in.*`/`adapter.out.*`, explicit instruction: "invoke `quarkus` skill." See table above.
   - Exact file paths to create / modify
   - TDD reminder: backend = mandatory failing test first, scoped per **Test Scope Policy** above — never a bare `./gradlew test`/`check`
   - **Commit footer:** the subagent's own worktree commits use a plain conventional-commit message only — no `Closes #NN` / `Epic: #YY` trailer. That footer is added exactly once, by the orchestrator, in Step 4 after the Review Gate returns Ready.
3. **Model selection:** sonnet by default; haiku for mechanical / well-specified tasks; opus only if the task explicitly specifies it (rare — opus is the orchestrator role).
4. **Receive subagent report.**
5. **Review Gate (before reabsorb) — non-breaking tasks only.** See Step 3.5. Breaking tasks skip this — `review-pr` reviews their PR instead.
6. **Reabsorb** the worktree branch into main per `superpowers:using-git-worktrees`. Parallel groups merge in dependency order. Conflicts → escalate to user.
7. **Cleanup:** `git worktree remove .claude/worktrees/worktree-<name> && git branch -d <temp-branch>`.

### Step 3.5: Review Gate (orchestrator-driven, before reabsorb)

A separate reviewer subagent QAs each completed task on its worktree branch
diff, before it touches main. Keeps main clean (Iron Law) and keeps QA out of
the orchestrator's context.

1. **Dispatch** via `superpowers:requesting-code-review`, overriding its
   template with `skills/execute-tasks/code-reviewer.md`. Fill placeholders:
   - `{TASK_ID}`, `{DESCRIPTION}`
   - `{ACCEPTANCE_CRITERIA}` — the TASKS.md section verbatim (that section only)
   - `{DIFF_RANGE}` — `main...<worktree-branch>`
   - `{SKILL_RULES}` — *names* of applicable cuts (e.g. "JAX-RS Resource rules
     from `backend/SKILL.md`; TDD mandate"). Never paste full SKILL files.
2. **Model:** haiku for mechanical / well-specified diffs (renames, config,
   plain DTOs); sonnet for diffs with real logic, concurrency, or security
   surface. Orchestrator picks per dispatch.
3. **Act on the verdict:**
   | Verdict | Action |
   |---|---|
   | Critical or Important present | Delta brief with **all** issues (incl. Minor) → SAME worktree subagent → re-review. Max **3 rounds**, then escalate to user. |
   | Minor only | One fix pass to the same worktree, **no re-review**; confirm the fix commit exists, then reabsorb. |
   | Ready (clean) | Reabsorb. |
4. **Record** any Minor items and their fixes in the commit body + SESSION.md.

Token discipline: the reviewer starts cold and sees only diff + criteria + rule
names + description — never orchestrator history, full SKILLs, or full TASKS.md.

### What the orchestrator does NOT do
- Edit / Write / NotebookEdit on source code
- Run `./gradlew compileJava` / `test` for production verification (subagent runs these and reports)
- Read source files to "just check" — that's a subagent job; reading for state (TASKS.md, SESSION.md, docs) is fine

### What the orchestrator DOES
- Reads docs (TASKS, ADRs, epic spec) to build the brief
- Writes the brief, dispatches, reviews the returned diff
- Manages TASKS/SESSION/GitHub state
- Commits + pushes after reabsorb
- Runs `gh` commands for project automation

## Step 4: Per-Task Completion (orchestrator)
1. Confirm the Step 3.5 Review Gate returned **Ready** (acceptance criteria already mapped to the diff by the reviewer) — `superpowers:verification-before-completion`. Trust the gate verdict; do not re-read source to re-derive criteria. If the gate never cleared (cap hit, escalation), do not commit — surface to user.
2. Update TASKS file: `[~]` → `[x]`
3. Commit using the footer format below
4. **Push immediately: `git push`** — `Closes #NN` processing requires push; Project automation sets Status → Done
5. **Loop decision — MANDATORY, executed in the SAME turn as the push, before ending the turn:**
   - Remaining `[ ]` tasks AND context <95% AND no usage-limit `<system-reminder>` received
     → **immediately invoke Step 1b for the next `[ ]` task in this same turn.** Do NOT end the turn. Do NOT write a status summary. Do NOT suggest `/execute-tasks resume`. Do NOT ask user for confirmation.
   - Remaining `[ ]` tasks AND (context ≥95% OR usage-limit warning) → Usage Limit protocol (Step "Handling the Usage Limit").
   - No `[ ]` remaining → Step 6 (Epic Close-Out).

### Red flags — STOP and resume looping if you catch yourself writing any of these between tasks of the same run:
- "Run `/execute-tasks resume` to continue"
- "Next: T-XX." as a turn-ending line
- "Awaiting your go-ahead" / "Let me know when to continue" / "Ready for next task?"
- A bullet-list completion summary followed by silence
- Setting SESSION.md `Phase: IDLE` or `IDLE-AWAITING-USER` while `[ ]` tasks remain and no halt condition fired

**All of these mean: you broke loop discipline. The correct action is to immediately dispatch the next `[ ]` task in this same turn.** The user invoked continuous mode; they explicitly do not want per-task confirmation. Pausing wastes their time and your context.

### Commit footer format (mandatory)
```
feat|fix|refactor|chore(scope): <gitmoji> short description

Body explaining why.

Closes #<task-issue-number>
Epic: #<epic-issue-number>
Co-Authored-By: <subagent-model> <noreply@anthropic.com>
```
- `scope` = BC or module name, not a ticket number
- `Co-Authored-By` = model that actually wrote the code (the subagent), not opus
- Extract numbers from TASKS headings: `## T-XX: ... [ ] (#NN)` and `**Epic Issue:** #YY`
- Never commit generated files, build artifacts, or `.env`
- For `build.gradle` changes, ask the user (CLAUDE.md rule)

### Git workflow (worktree-always)
| Condition | Flow |
|---|---|
| Any task (1 or N) | Worktree(s) under `.claude/worktrees/worktree-<name>`; subagent commits in worktree; orchestrator reabsorbs into main and pushes |
| Task marked `breaking` | Apply **Expand-Contract (Parallel Change)**: add new path alongside old → migrate callers → remove old. One PR per phase, each with review gate. Big-bang single PR only when no consumer exists. For phased removal of an existing endpoint/flow, use **Strangler Fig**: route new calls to replacement, mark old deprecated, remove after grace period. Each phase = separate subagent dispatch. |

## Step 5: Verification (orchestrator commands; or delegate if heavy)

One run per task, per **Test Scope Policy** above — not a repeat of what the subagent already ran mid-loop.
- After reabsorb on main: `./gradlew compileJava compileTestJava` — zero errors
- `./gradlew test` **and** `./gradlew quarkusRuntimeTest`, both scoped to the touched BC's test classes — never the bare, unscoped form of either
- For any backend task: always include `--tests "de.ingoschindler.architecture.*"` alongside the BC-scoped filters (on the `test` task — `HexagonalArchitectureTest` is plain ArchUnit, no `@QuarkusTest`). BC-scoped filters alone never exercise the ArchUnit fitness functions — a layering violation (e.g. a use case injecting `EntityManager` or calling a Panache entity's static finder directly) passes every BC suite and only fails `HexagonalArchitectureTest`. Skipping this is how such a violation reaches main: it passes every BC suite and is only caught after the merge.
- Heavy / long-running suites: dispatch a verification subagent instead of running in orchestrator
- Every completed task has `[x]`; report remaining `[ ]` / `[!]`

## Step 6: Epic Close-Out (when all tasks done)
1. **Close Milestone:**
   ```bash
   gh api repos/{REPO}/milestones/{milestone_number} --method PATCH -f state=closed
   ```
   Find number: `gh api repos/{REPO}/milestones --jq '.[] | {number, title}'`
2. **GitHub Projects — Epic issue to Done:** using `github-projects` SKILL, set the Epic issue's Status field to `{STATUS_DONE}`. If the call fails, surface the error.
3. **Epic file** (`docs/epics/E{n}-*.md`): add or update `**Status:** Delivered` immediately below the `# E{n}:` heading, then `git mv` it to `docs/epics/done/E{n}-*.md`.
   **The move changes the file's depth, so relative links break in both directions — fix both before committing:**
   - *Links inside the moved file* now sit one level too shallow: `../adr/…` → `../../adr/…`, `../guidelines/…` → `../../guidelines/…`, `../PRODUCT-ARCHITECTURE.md` → `../../PRODUCT-ARCHITECTURE.md`, and `../../.claude/…` → `../../../.claude/…`.
   - *Links pointing at the moved file* from ADRs and `docs/epics/tasks/E{n}-TASKS.md` still name the old location: `../epics/E{n}-*.md` → `../epics/done/E{n}-*.md`, `../E{n}-*.md` → `../done/E{n}-*.md`.

   Verify, do not eyeball — this check catches both directions at once:
   ```bash
   python3 - <<'PY'
   import pathlib, re
   pat = re.compile(r"\]\((\.\.?/[^)#\s]+)\)")
   for root in ("docs", ".claude"):
       for p in pathlib.Path(root).rglob("*.md"):
           for m in pat.finditer(p.read_text()):
               if not (p.parent / m.group(1)).resolve().is_file():
                   print(f"BROKEN {p}: {m.group(1)}")
   PY
   ```
   Two known non-findings it will print: `define-epic/SKILL.md`'s `ADR-{nn}-{slug}.md`
   template placeholder, and E26's link to an idea that was deleted on promotion.
   Skipping this left **144 broken links across 46 files** to be repaired in one sweep on 2026-08-14 — every epic delivered since `done/` was introduced had lost its ADR links.
4. **New ADRs from this epic:** for every `docs/adr/ADR-*.md` with `**Epic:** E{n}`, flip `Status: Proposed` (or `Accepted`) → `Status: Delivered` — skip any ADR handled by step 6 below instead.
5. **ROADMAP.md:** convert the epic's "Scope" subsection to "Delivered" (past tense, factual); update the epic's link path to `docs/epics/done/E{n}-*.md` to match step 3
6. **Superseded ADRs:** for every ADR referenced by a `**Supersedes:**` field on one of this epic's new ADRs, set the *old* ADR's own `**Status:**` to `Superseded` and add `**Superseded-by:** [ADR-{n}: {title}](ADR-{n}-{slug}.md)` immediately below it — only now, once the replacement has actually landed, never at spec time.
7. **Commit:**
   ```
   chore(docs): close E{n} epic in ROADMAP — convert Scope to Delivered
   ```
8. **Completion path:** invoke `superpowers:finishing-a-development-branch`

## Handling the Usage Limit (reactive halt)

**Trigger:** any `<system-reminder>` containing a usage/rate limit warning, OR observable degradation (responses slowing, tool outputs stacking, repeated retries). The injected reminder is the authoritative signal — react immediately.

**Protocol on warning arrival:**

1. **Stop dispatching.** No new subagent goes out. No new worktree gets created.
2. **In-flight subagent: let it finish.** Its context budget is separate from yours; aborting risks losing committed-but-unreabsorbed work. Wait for the return, review the diff, reabsorb into main, push. This completes the current task at a clean boundary.
3. **Finalize state on main:**
   - TASKS.md: completed task `[~]` → `[x]`; next planned task stays `[ ]`
   - GitHub Projects: Status → Done for the just-finished task (via `github-projects`)
   - Commit + push the completion
4. **Update `.claude/SESSION.md`:**
   - `Phase: IDLE-AWAITING-USER`
   - `In-flight: none — halted on limit warning`
   - `Next step: dispatch T-XX (next planned task from E{n}-TASKS.md)`
   - `Reset: <ISO timestamp from warning, if present>`
   - Commit + push on main
5. **Inform user — one terse message:**
   > "Limit warning received. Halted after T-XX. Reset: `<Xh>`. Resume via `/execute-tasks resume` or `/execute-tasks continue` when ready."
6. **Stop.** Do not poll, do not retry, do not auto-resume.

**If reabsorb itself would burn too many tokens** (large diff, merge conflict expected): skip step 2's reabsorb. Subagent commits in worktree, worktree stays. SESSION.md `Next step: reabsorb worktree-<name>, then dispatch T-YY`. User-visible message names the worktree.

## Resume Protocol

Triggered by `/execute-tasks resume` or `/execute-tasks continue` (orchestrator should treat both identically).

1. SessionStart hook has already surfaced SESSION.md.
2. Read `Phase: IDLE-AWAITING-USER` → confirm prior halt state.
3. If `Next step` names a pending reabsorb of a worktree → do that first (review diff, merge, push, cleanup), then continue.
4. If `Next step` names a fresh dispatch → run Step 1b kickoff for that task, then dispatch as normal.
5. Update SESSION.md `Phase` away from IDLE-AWAITING-USER as soon as the first new dispatch goes out.

**No auto-resume.** If SessionStart surfaces `Phase: IDLE-AWAITING-USER` and the user's first message is NOT a resume command, do nothing task-related — answer whatever they asked, leave halt state intact.

## Rules
- **Never silently reinterpret acceptance criteria or change scope.** If a criterion is ambiguous, unverifiable, or appears wrong under implementation, stop and ask — don't bend the test or the criterion to make green.
- If `gh` commands fail, log warning and continue — TASKS file is source of truth
- If a task heading lacks `(#NN)`: `gh issue list -R {REPO} --search "{EPIC_ID} T-{nn}" --json number`
- Worktree location: `.claude/worktrees/worktree-<name>` (never `../`)
- Orchestrator never edits source files. See Iron Law.
