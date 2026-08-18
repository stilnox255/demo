---
name: capture-idea
description: Use when the user has a rough idea, improvement suggestion, or "nice-to-have" that isn't ready to become an epic, or says things like "notier das mal", "irgendwann mal könnten wir", "vielleicht sollten wir", "halte das fest als Idee", "improvement suggestion".
model: sonnet
---

Capture an idea as **Product Discovery** — rough, low-commitment, marinate-or-discard. Target: ≤2 minutes, no GitHub sync, no ADRs, no brainstorming sub-skill.

## When to use vs. `define-epic`

- **`capture-idea`**: problem isn't sharp, scope unclear, no commitment to ship. Output: markdown only. Pure brain-dump — "ich glaube wir brauchen X in zukunft".
- **`define-epic`**: goal is clear and the user is committing to plan + execute. Output: epic file + ADRs + issues + milestone.

If in doubt → `capture-idea`. Promotion happens later via `define-epic` reading the idea file as input; the idea file is then deleted — the epic supersedes it.

## Step 1: Slug & duplicate check
- The filename is `docs/ideas/{slug}.md`, where `{slug}` is a short kebab-case form of the title. **Ideas carry no number.** The companion wiki numbers them `I-NN` on ingest and is the permanent register; the repo holds only the open ones and deletes each on promotion. A repo number would be handed to the next capture and drift from the wiki's — it did, until `I-08` named three different ideas. See `docs/superpowers/specs/2026-08-14-ideas-without-numbers-design.md`.
- Pick a slug that will still identify the idea in a link a year from now, and check `docs/ideas/` for a name collision.
- Grep a keyword from the user's input across `docs/ideas/` and `docs/epics/E*-*.md` (local files, not GitHub issues). If a close match surfaces, ask whether to update that file instead of creating a new one.

## Step 2: Capture (brain-dump)

If the user's trigger message already contains the idea, **skip the question** — extract a short title and body from the message and propose them for confirmation.

Otherwise ask once:
> Kurz — was ist die Idee? (Titel + ein paar Sätze reichen)

Accept anything from a single sentence to a paragraph. No fixed fields, no checklists, no promotion criterion, no area/size tagging.

Do NOT: demand user story, scope matrix, success metrics, alternatives analysis, ADRs. This is a brain-dump — "ich glaube wir brauchen X" ist genug.

## Step 3: Write file
`docs/ideas/{kebab-slug}.md` (slug ≤ 5 words, lowercase, hyphens) — flat markdown, no frontmatter. Create `docs/ideas/` if missing:

````markdown
# {Title}

_Created: {YYYY-MM-DD}_

{brain-dump body}
````

No required sections. Later updates append text or add headings organically.

## Step 4: Update index
If `docs/ideas/README.md` exists, append one line at the **end** of the list under `## Ideas` — appending keeps capture order without needing a sort rule. If missing, create it with a single `## Ideas` heading followed by this first entry:
```
- [{Short title}]({slug}.md)
```
Titles are enough — Claude reads the files when detail is needed.

## Step 5: Commit (no push)
```bash
git add docs/ideas/
git commit -m "docs(ideas): capture {slug} — {short title}"
```

**Do NOT push.** Ideas accumulate locally; the user pushes deliberately when they want to share.

## Follow-up workflows (ad-hoc, no sub-skills)

These are plain file ops Claude handles directly when the user asks — no dedicated skills needed.

- **Update / extend** — "ergänze edge-deployment mit …" → read the file, append the new thoughts (as prose or a new heading). Commit: `docs(ideas): extend {slug} — {what}`.
- **Split** — "das ist zu komplex, splitte in zwei" → propose a split, user confirms titles. Create two new ideas (with their own slugs), delete the original file + its index line. Commit: `docs(ideas): split {slug} → {slug-a}, {slug-b}`.
- **Assess ripeness** — "welche idee ist reif fürs epic?" → glob `docs/ideas/*.md`, summarize each, nominate candidates for `define-epic`.
- **Discard** — "das ist erledigt" / "obsolet" → delete the file + its index line. No reason required — gut feeling is enough. Commit: `docs(ideas): discard {slug}`.
- **Promote** — handled by `define-epic`; it reads the idea file as input, then deletes both the file and its index line in the same commit as the epic creation. The epic does not reference the idea — it stands on its own.

If anyone later wants to find an old idea, `git log --diff-filter=D -- docs/ideas/` surfaces all deleted idea files with their full content.

## Output
- `docs/ideas/{slug}.md`
- One-line index entry in `docs/ideas/README.md` (if the index exists)
- **Nothing on GitHub**. No issue, no milestone, no project item.

## Rules
- No ADRs, no GitHub issues, no milestones, no `ROADMAP.md` entry.
- Ideas are allowed to die. On discard or promotion the file **and** its index line are deleted — git is the archive. No status tracking, no tombstones, no reason required.
- Ideas have no number. The slug is the identifier, and it never changes, so a link written today still resolves after the wiki renumbers or the file is deleted.
- All content in English; trigger phrases in the description may be bilingual.
- If the idea clearly overlaps an open epic → suggest extending that epic's scope instead of creating an idea file.
