# Engineering Guidelines (binding)

## Always-on

Drop a policy file here and `@`-import it below to make it binding for every code
generation, edit, and review. The path is relative to **this file**, not the repo
root, so write `@my-policy.md`, not `@docs/guidelines/my-policy.md`. A wrong path is
ignored without any error, so verify with `/context` in a fresh session that the file
shows up under *Memory files*.

## Contextual (loaded on demand as skills)

For a guideline that should only apply in specific contexts, drop the file here and
create a skill in `.claude/skills/<name>/SKILL.md` whose `description:` is the
trigger, and have it read the file.
