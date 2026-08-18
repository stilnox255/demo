# Engineering Guidelines

**This directory is empty by design.** The starter ships no policy files; the
decisions that bind are the ADRs in `docs/adr/`. What follows is how to add one if
your project needs it.

## Always-on

Drop a policy file here, `@`-import it below, and `@`-import *this* file from the
repository-root `CLAUDE.md` — that last step is what makes the chain load, and the
root file does not do it while there is nothing to load. The path is relative to
**this file**, not the repo root, so write `@my-policy.md`, not
`@docs/guidelines/my-policy.md`. A wrong path is ignored without any error, so verify
with `/context` in a fresh session that the file shows up under *Memory files*.

## Contextual (loaded on demand as skills)

For a guideline that should only apply in specific contexts, drop the file here and
create a skill in `.claude/skills/<name>/SKILL.md` whose `description:` is the
trigger, and have it read the file.
