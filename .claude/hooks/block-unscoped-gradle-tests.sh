#!/usr/bin/env bash
# Blocks bare/unscoped gradlew build|check|test|quarkusRuntimeTest invocations:
# `build`/`check` always drag in the full quarkusRuntimeTest suite, and running
# `test`/`quarkusRuntimeTest` without --tests runs every test in that task.
set -euo pipefail

payload=$(cat)
command=$(echo "$payload" | jq -r '.tool_input.command // empty')

[ -z "$command" ] && exit 0

# Only look at gradlew invocations.
if ! echo "$command" | grep -qE '(^|[[:space:]/])gradlew([[:space:]]|$)'; then
    exit 0
fi

block() {
    echo "Blocked: $1" >&2
    echo "Use --tests to scope the run." >&2
    exit 2
}

# 'build' or 'check' as a standalone gradle task name anywhere in the command.
if echo "$command" | grep -qE '(^|[[:space:]])(build|check)([[:space:]]|$)'; then
    block "'gradlew build'/'check' always run the full quarkusRuntimeTest suite."
fi

# 'test' or 'quarkusRuntimeTest' as a task without --tests anywhere in the command.
if echo "$command" | grep -qE '(^|[[:space:]])(test|quarkusRuntimeTest)([[:space:]]|$)'; then
    if ! echo "$command" | grep -q -- '--tests'; then
        block "unscoped 'test'/'quarkusRuntimeTest' run (no --tests) runs every test in that task."
    fi
fi

exit 0
