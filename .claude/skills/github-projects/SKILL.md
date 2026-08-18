---
name: github-projects
description: Use when a skill needs to sync with GitHub Projects V2 — provides GraphQL templates for status updates, iteration assignment, sub-issue management, and issue creation.
---

## Configuration

Read from CLAUDE.md `GitHub Project Integration` section:

| Variable | CLAUDE.md field |
|----------|-----------------|
| `{REPO}` | `Repository:` (`owner/repo`) |
| `{ORG}` | owner part of `{REPO}` |
| `{PROJECT_NUMBER}` | last segment of project URL |
| `{PROJECT_ID}` | `Project ID:` |
| `{STATUS_FIELD_ID}` | `Status field ID:` |
| `{ITERATION_FIELD_ID}` | `Iteration field ID:` |
| `{STATUS_BACKLOG/READY/IN_PROGRESS/IN_REVIEW/DONE}` | option IDs from `Status options:` |

If no GitHub Project Integration section exists in CLAUDE.md, skip all project sync — TASKS.md is source of truth. If CLAUDE.md configures a different tracker (Linear etc.), adapt accordingly.

All operations are **best-effort** — append `|| true` and log failures without stopping.

## GraphQL Templates

### Get item ID (by issue number, via repository)
```bash
gh api graphql -f query="{
  repository(owner: \"{ORG}\", name: \"{REPO_NAME}\") {
    issue(number: {ISSUE_NR}) {
      projectItems(first: 5) { nodes { id project { number } } }
    }
  }
}" --jq ".data.repository.issue.projectItems.nodes[] | select(.project.number == {PROJECT_NUMBER}) | .id"
```

### Get item ID (by issue number, via organization — for issues not yet in project)

> **Prefer the repository-scoped query above.** The `items` connection is capped
> at `first: 100`; on a project with more than 100 items this query returns
> `EXCESSIVE_PAGINATION` and `projectV2: null`. The repository-scoped query
> (`repository.issue.projectItems`) works regardless of project size — use it
> per issue number instead of scanning the whole project.

```bash
gh api graphql -f query='{
  organization(login: "{ORG}") {
    projectV2(number: {PROJECT_NUMBER}) {
      items(first: 100) { nodes { id content { ... on Issue { number } } } }
    }
  }
}' --jq ".data.organization.projectV2.items.nodes[] | select(.content.number == {ISSUE_NR}) | .id"
```

### Set status

> **The gh CLI param is always `-f query=` — even for mutations.** `gh api
> graphql` only recognises a parameter named `query`; passing `-f mutation=`
> fails with `A query attribute must be specified and must be a string`. The
> GraphQL `mutation { ... }` keyword stays *inside* the string.

```bash
gh api graphql -f query="mutation {
  updateProjectV2ItemFieldValue(input: {
    projectId: \"{PROJECT_ID}\" itemId: \"{ITEM_ID}\"
    fieldId: \"{STATUS_FIELD_ID}\"
    value: { singleSelectOptionId: \"{STATUS_OPTION_ID}\" }
  }) { projectV2Item { id } }
}" || true
```

### Add issue to project (returns item ID)
```bash
gh project item-add {PROJECT_NUMBER} --owner {ORG} \
  --url "https://github.com/{REPO}/issues/{NR}" \
  --format json | jq -r '.id'
```

### Set iteration
```bash
gh api graphql -f query="mutation {
  updateProjectV2ItemFieldValue(input: {
    projectId: \"{PROJECT_ID}\" itemId: \"{ITEM_ID}\"
    fieldId: \"{ITERATION_FIELD_ID}\"
    value: { iterationId: \"{ITERATION_ID}\" }
  }) { projectV2Item { id } }
}" || true
```

### List available iterations
Execute as two separate Bash calls — (1) `date +%Y-%m-%d`, then (2) the query
below.

> **Pass the query from a file**, not as an inline multi-line `-f query="..."`
> string. The inline form mangles the `... on ProjectV2IterationField` inline
> fragment in some shells and fails with `Expected NAME, actual: (none)`. Writing
> the query to a file and passing `-F query=@file` is reliable.
>
> `configuration.iterations` holds **only current and upcoming** iterations — it
> is empty when no future iteration is defined. Past iterations live under
> `completedIterations`. If `iterations` is empty, there is nothing to assign:
> skip iteration assignment (best-effort) and report it to the user.

```bash
cat > /tmp/gh-iterations.graphql <<'EOF'
query {
  organization(login: "{ORG}") {
    projectV2(number: {PROJECT_NUMBER}) {
      field(name: "Iteration") {
        ... on ProjectV2IterationField {
          configuration {
            iterations { id title startDate }
            completedIterations { id title startDate }
          }
        }
      }
    }
  }
}
EOF
gh api graphql -F query=@/tmp/gh-iterations.graphql \
  --jq '.data.organization.projectV2.field.configuration.iterations[]'
```

### Issue dependencies

Add a `blocked_by` dependency (issue_id is the **integer** database ID of the blocker):
```bash
gh api repos/{REPO}/issues/{TASK_NR}/dependencies/blocked_by \
  --method POST -H "X-GitHub-Api-Version: 2026-03-10" \
  -F issue_id={BLOCKER_INT_ID} || true
```

Remove a `blocked_by` dependency:
```bash
gh api repos/{REPO}/issues/{TASK_NR}/dependencies/blocked_by/{BLOCKER_INT_ID} \
  --method DELETE -H "X-GitHub-Api-Version: 2026-03-10" || true
```

List blockers of an issue:
```bash
gh api repos/{REPO}/issues/{TASK_NR}/dependencies/blocked_by \
  -H "X-GitHub-Api-Version: 2026-03-10" --jq '.[].number'
```

### Issue ID types
The sub-issues and dependencies REST APIs require the **integer database ID** (`id` from REST API), not the GraphQL node ID.

Fetch the integer ID for an issue:
```bash
gh api repos/{REPO}/issues/{NR} --jq '.id'
```

Fetch both IDs in one call (use when building the issue mapping):
```bash
gh api repos/{REPO}/issues/{NR} --jq '{number: .number, intId: .id, nodeId: .node_id}'
```

- `intId` → integer, required by sub-issues REST API
- `nodeId` → GraphQL node ID (`I_kwDO…`), required by `addProjectV2ItemById` and other GraphQL mutations

### Add sub-issue to epic
`sub_issue_id` must be the **integer** database ID. Use `-F` (not `-f`) to send it as a JSON number.
```bash
gh api repos/{REPO}/issues/{EPIC_NR}/sub_issues \
  --method POST -H "X-GitHub-Api-Version: 2026-03-10" \
  -F sub_issue_id={TASK_ISSUE_INT_ID} || true
```

### Remove sub-issue from epic
Note: endpoint path is `sub_issue` (singular), not `sub_issues`.
```bash
gh api repos/{REPO}/issues/{EPIC_NR}/sub_issue \
  --method DELETE -H "X-GitHub-Api-Version: 2026-03-10" \
  -F sub_issue_id={TASK_ISSUE_INT_ID} || true
```

### Prioritize sub-issue (place after another)
`sub_issue_id` and `after_id`/`before_id` are all integer database IDs. Use `-F` for all three.
```bash
gh api repos/{REPO}/issues/{EPIC_NR}/sub_issues/priority \
  --method PATCH -H "X-GitHub-Api-Version: 2026-03-10" \
  -F sub_issue_id={CURRENT_INT_ID} -F after_id={PREVIOUS_INT_ID} || true
```
