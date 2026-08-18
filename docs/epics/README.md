# Epics

One file per epic, created by the `/define-epic` skill:

```
docs/epics/
├── E{n}-{slug}.md          the epic: Goal, Motivation, User Story, Success Signal,
│                           Scope (In/Out), API/UX, Architecture Decisions, Scope by
│                           component
├── tasks/E{n}-TASKS.md     the task breakdown, created by /plan-epic
└── done/                   delivered epics, moved here by /execute-tasks
```

An epic states what changes and why. The decisions it makes along the way go into
`docs/adr/` and are linked from the epic — never written inline, because a decision
buried in a delivered epic is a decision nobody finds again.
