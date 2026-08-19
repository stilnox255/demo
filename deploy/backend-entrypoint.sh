#!/bin/bash
# Turns Docker secret *files* into the environment variables the application
# reads. Quarkus can read a config value from the environment but not from a
# file, and putting a password in the compose file or in `docker inspect` output
# is the thing secrets exist to avoid.
#
# $(cat ...) also strips the trailing newline that `openssl rand ... > file`
# leaves behind. That newline is invisible in every editor and makes Postgres
# answer "password authentication failed".
set -euo pipefail

read_secret() {
    local var_name="$1" file_var="$2" file
    file="${!file_var:-}"
    if [ -z "$file" ]; then
        echo "entrypoint: $file_var is not set" >&2
        exit 1
    fi
    if [ ! -r "$file" ]; then
        # Almost always a permission problem rather than a missing file: the
        # container UID is not the host owner, so the secret needs to be
        # world-readable on the host (setup-secrets.sh writes 0644 inside a 0700
        # directory). Fail loudly here instead of booting into a confusing
        # authentication error later.
        echo "entrypoint: cannot read $file (referenced by $file_var) — check ownership and mode" >&2
        exit 1
    fi
    export "$var_name=$(cat "$file")"
}

read_secret QUARKUS_DATASOURCE_PASSWORD QUARKUS_DATASOURCE_PASSWORD_FILE
read_secret S3_ACCESS_KEY S3_ACCESS_KEY_FILE
read_secret S3_SECRET_KEY S3_SECRET_KEY_FILE
read_secret DOWNLOAD_TOKEN_SECRET DOWNLOAD_TOKEN_SECRET_FILE

# Redis takes its credentials inside the connection URL, so the password cannot
# simply be exported under its own name — the URL has to be assembled here.
# Reserved characters are percent-encoded, or a generated password containing
# `@` or `/` silently produces a URL pointing somewhere else.
if [ -n "${REDIS_PASSWORD_FILE:-}" ] && [ -r "$REDIS_PASSWORD_FILE" ]; then
    REDIS_PASSWORD_RAW="$(cat "$REDIS_PASSWORD_FILE")"
    REDIS_PASSWORD_ENC="$(printf %s "$REDIS_PASSWORD_RAW" \
        | sed -e 's|%|%25|g' -e 's|@|%40|g' -e 's|/|%2F|g' -e 's|:|%3A|g' -e 's|?|%3F|g' -e 's|#|%23|g')"
    export QUARKUS_REDIS_HOSTS="redis://:${REDIS_PASSWORD_ENC}@${REDIS_HOST:-redis}:${REDIS_PORT:-6379}"
fi

exec java -jar /deployments/quarkus-run.jar
