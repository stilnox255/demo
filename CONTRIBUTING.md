# Contributing

## Git hooks (one-time per clone)

This repo ships a versioned pre-commit hook in `.githooks/`. Enable it once after
cloning:

```bash
git config core.hooksPath .githooks
```

The pre-commit hook is **verify-only**: it runs `spotlessCheck`, `checkstyleMain`,
and `checkstyleTest`, and blocks the commit on any violation. It does not modify
files.

Fix formatting before committing:

```bash
./gradlew spotlessApply
```

Checkstyle violations must be fixed by hand, then re-commit.

To bypass the hook in an emergency:

```bash
git commit --no-verify
```
