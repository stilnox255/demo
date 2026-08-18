#!/bin/bash
# Runs once, on an empty data directory, from the Postgres image's
# docker-entrypoint-initdb.d hook.
#
# Creates the application role and its database so the application never
# connects as the superuser. Flyway then owns the schema inside that database —
# this script deliberately creates no tables, because two things creating schema
# is one thing too many.
set -euo pipefail

APP_DB_NAME="${APP_DB_NAME:-starter}"
APP_DB_USER="${APP_DB_USER:-starter}"
APP_DB_PASSWORD="$(cat "$APP_DB_PASSWORD_FILE")"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER ${APP_DB_USER} WITH PASSWORD '${APP_DB_PASSWORD}';
    CREATE DATABASE ${APP_DB_NAME} OWNER ${APP_DB_USER};
    GRANT ALL PRIVILEGES ON DATABASE ${APP_DB_NAME} TO ${APP_DB_USER};
EOSQL

echo "init-db: created database ${APP_DB_NAME} owned by ${APP_DB_USER}"
