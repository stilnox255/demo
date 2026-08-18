#!/bin/bash
# Grafana wrapper for the optional monitoring profile.
#
# Its only job: re-export the database password without the trailing newline.
# Grafana's own `$__file{/run/secrets/db_password}` interpolation in a datasource
# YAML takes the file content verbatim, newline included, and Postgres then
# rejects the login with "password authentication failed" — which reads like a
# wrong password rather than a whitespace problem, and costs an hour to find.
set -euo pipefail

if [ -n "${APP_DB_PASSWORD_FILE:-}" ] && [ -r "$APP_DB_PASSWORD_FILE" ]; then
    export APP_DB_PASSWORD="$(cat "$APP_DB_PASSWORD_FILE")"
fi

# The image's own entrypoint, which translates GF_* variables into grafana.ini.
exec /run.sh "$@"
