# OBEY Modern Software Engineering by Dave Farley

## Purpose

This repository follows **Modern Software Engineering** in the sense of Dave Farley:
treat software development as a genuine *engineering discipline* — the application of
an empirical, scientific approach to the practical problem of building software that
works, economically and at scale.

Engineering here is not heavyweight process, ceremony, or documentation. It is the
disciplined pursuit of two things:

1. **Becoming experts at learning** — so we discover what to build and how, fast.
2. **Becoming experts at managing complexity** — so the systems we build stay tractable as they grow.

This file is a binding engineering policy: `MUST` is binding, `SHOULD` is a strong
default, and `MUST NOT` is forbidden.

---

## Primary Directive

Software engineering is the application of an **empirical, scientific approach** to
finding **efficient, economic** solutions to **practical** problems in software.

When uncertain, prefer the path that:
1. produces faster, higher-quality feedback
2. lets you take a smaller, safer, reversible step
3. reduces the complexity a human must hold in their head
4. keeps the system testable and deployable at all times
5. treats a belief as a hypothesis to be tested, not a truth to be defended

Opinion, fashion, and seniority do not settle technical questions. Evidence does.

---

## The Two Core Competencies

All techniques below serve one of two goals. Tag every significant decision with which
one it serves; if it serves neither, question it.

- **Optimize for Learning** — iterative, incremental, feedback-driven, experimental, empirical.
- **Manage Complexity** — modularity, cohesion, separation of concerns, abstraction, loose coupling.

---

## Experts at Learning

### Work Iteratively
1. Treat the first version of anything as wrong and plan to change it.
2. Make progress through repeated cycles of small change + evaluation.
3. Do not attempt to specify the whole solution up front and build it once.
4. Prefer a working, imperfect increment now over a perfect design later.

### Work Incrementally
1. Build and integrate the system in small, independently valuable pieces.
2. Each increment SHOULD be releasable, even if not released.
3. Avoid long-lived work that cannot be integrated or evaluated until "done".

### Use Fast, High-Quality Feedback
1. Feedback is the central tool of engineering — shorten every feedback loop you can.
2. Required feedback loops, fastest first: editor/compile, unit test, acceptance test, integration, deployment, production telemetry.
3. Slow feedback is a defect to be fixed, not a condition to be tolerated.
4. Ambiguous or flaky feedback is worse than slow feedback — fix or delete it.

### Be Experimental
1. Frame changes as hypotheses with an expected, observable outcome.
2. Control the variables: change one meaningful thing at a time when diagnosing.
3. Measure the result; let the measurement, not the intention, decide.
4. "It compiles / it looks right" is not evidence it works.

### Be Empirical
1. Ground decisions in observation of the real system, not in models of it.
2. When reality and the plan disagree, reality wins.
3. Reproduce a problem before fixing it; verify the fix against the reproduction.

Anti-patterns (MUST NOT):
- big-bang design or big-bang integration
- long-lived feature branches that defer integration feedback
- guessing at causes instead of measuring
- defending a design against contradicting evidence

---

## Experts at Managing Complexity

### Modularity
1. Decompose the system into parts that can be understood and changed in isolation.
2. A module boundary MUST hide a decision that is likely to change.
3. Good modularity is judged by how little you must know about the rest of the system to change one part.

### Cohesion
1. Things that change together belong together.
2. Keep a module focused on one responsibility; move unrelated concerns out.
3. Low cohesion (a module that does many unrelated things) is a defect.

### Separation of Concerns
1. Each piece of code SHOULD address one concern; keep distinct concerns apart.
2. Separate *what* from *how*, policy from detail, essential complexity from accidental.
3. Do not interleave business logic with I/O, framework, or transport detail.

### Abstraction (Information Hiding)
1. Expose intent through interfaces; hide implementation behind them.
2. Choose abstractions that let a reader reason without opening the implementation.
3. Leaky abstractions that force callers to know internals are a defect.

### Managing Coupling
1. Prefer loose coupling between modules; minimize what one must know about another.
2. Reduce the number and reach of dependencies; depend on abstractions, not details.
3. Distinguish essential coupling (inherent to the problem) from accidental coupling (introduced by us) and remove the latter.
4. Beware coupling through shared mutable state, shared data schemas, and synchronous chains.

Anti-patterns (MUST NOT):
- god modules that know and do everything
- shotgun surgery: one change forcing edits across many modules
- abstractions chosen for reuse before intent is clear
- hidden coupling through global/shared state

---

## Tools of the Engineering Mind

Apply these reasoning tools deliberately; they are how the two competencies are achieved.

1. **Testability** — if it is hard to test, the design is telling you it is too coupled or unclear. Treat testability pressure as design feedback, not a chore.
2. **Deployability** — if it is hard to deploy, the design or the pipeline is too coupled. Keep the system always deployable.
3. **Speed** — favor approaches that keep feedback fast; slowness compounds.
4. **Controlling variables** — isolate what you change so you can attribute outcomes.
5. **Continuous Delivery** — work so the software is *always* in a releasable state; this is a forcing function for everything above.

---

## Testability and Deployability as Design Drivers

1. Design *for* testability — it is a proxy for modularity, cohesion, separation of concerns, and loose coupling.
2. If a test is hard to write, fix the design before fixing the test.
3. Tests SHOULD specify behavior (the *what*), not pin implementation (the *how*).
4. Keep the pipeline as the arbiter of "done": if it does not pass the pipeline, it is not done.
5. Deployability problems are design problems — address them in the design, not by manual workarounds.

Anti-patterns (MUST NOT):
- tests coupled to implementation detail that break on safe refactors
- "we'll add tests later" as a default
- manual deployment steps that the pipeline cannot reproduce

---

## Working in Small Steps

1. Prefer the smallest change that moves the system forward and keeps it working.
2. Integrate small changes continuously rather than batching them.
3. A change that cannot be made small is a signal of excessive coupling — reduce the coupling first.
4. Keep the build green; a red build is a stop-the-line event.

Anti-patterns (MUST NOT):
- large, unreviewable changes
- batching many concerns into one change
- leaving the mainline broken

---

## Engineering vs. Craft / Heavyweight Process

1. Do not equate engineering with bureaucracy, gated phases, or exhaustive up-front documentation.
2. Do not equate it with pure craft or individual heroics either.
3. Prefer empirical, incremental discipline that produces evidence over process that produces paperwork.
4. Practices exist to serve learning and complexity management; drop a practice that serves neither.

---

## Review Rules

When reviewing code or a design, actively look for:
- a change that could not be made small, and why
- modules that change for many unrelated reasons (low cohesion)
- concerns tangled together (logic + I/O + framework)
- leaky or premature abstractions
- accidental coupling, shared mutable state, long synchronous chains
- code that is hard to test, and what that reveals about the design
- anything that lengthens or muddies a feedback loop
- decisions defended by opinion rather than evidence

---

## Code Generation Rules

When generating code or proposing a design, default to:
1. the smallest increment that works and integrates
2. clear module boundaries that hide a likely-to-change decision
3. high cohesion and separation of concerns
4. dependencies on abstractions, minimal coupling
5. testable units, with tests that specify behavior
6. an always-deployable result
7. a stated hypothesis and an observable way to confirm it

Avoid by default:
- big up-front designs built in one shot
- god objects, tangled concerns, premature reuse abstractions
- implementation-coupled tests
- changes that break integration or deployment

---

## Final Instruction

When uncertain, prefer the design that:
1. shortens and sharpens feedback
2. can be reached in a small, reversible step
3. reduces the complexity a human must hold at once
4. stays testable and deployable
5. lets evidence, not opinion, decide

Engineering is learning fast and keeping complexity under control. Everything else is detail.
