#!/bin/bash
# pg_dump of the application database, gzipped, with retention.
#
# Two ways to run it:
#
#   1) Unattended, from cron on the deployment host:
#        0 3 * * * /srv/starter/backup-db.sh >>/var/log/starter-backup.log 2>&1
#
#   2) By hand, optionally with a label that ends up in the filename — useful
#      right before a risky change:
#        ./backup-db.sh                    -> starter-<ts>.sql.gz
#        ./backup-db.sh before-migration   -> starter-<ts>-before-migration.sql.gz
#
# The compose file also runs a scheduled backup container. This script is not a
# duplicate of it: it is the path an operator can run, label, and reason about
# during an incident, and it is what restore-db.sh reads.
#
# Why pg_dump and not a volume snapshot: a textual dump restores across Postgres
# minor versions and into a different host, which is the situation a restore is
# usually needed in. A snapshot is tied to the exact binary that wrote it.
#
# Why --clean --if-exists: a restore has to be applicable to an existing, possibly
# half-broken database. Why --no-owner: so it also restores into a database whose
# role names differ.
set -euo pipefail

readonly BACKUP_DIR="${STARTER_BACKUP_DIR:-/srv/starter/backups}"
readonly CONTAINER_NAME="${STARTER_POSTGRES_CONTAINER:-starter-postgres-1}"
readonly DB_NAME="${STARTER_DB_NAME:-starter}"
readonly DB_USER="${STARTER_DB_USER:-starter}"
readonly SECRET_FILE_IN_CONTAINER="${STARTER_DB_SECRET_FILE:-/run/secrets/db_password}"
readonly RETENTION_DAYS="${STARTER_BACKUP_RETENTION_DAYS:-14}"

mkdir -p "${BACKUP_DIR}"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "FATAL: postgres container '${CONTAINER_NAME}' is not running." >&2
    echo "       docker ps shows:" >&2
    docker ps --format '  {{.Names}}' >&2
    exit 1
fi

# Anything outside [alnum]._- becomes a dash, so the label cannot produce a
# filename that breaks later globbing — or a shell injection, since this value
# reaches a command line.
LABEL_SUFFIX=""
if [[ $# -ge 1 && -n "$1" ]]; then
    sanitized="$(printf %s "$1" | tr -c '[:alnum:]._-' '-')"
    sanitized="${sanitized#-}"
    sanitized="${sanitized%-}"
    if [[ -n "${sanitized}" ]]; then
        LABEL_SUFFIX="-${sanitized}"
    fi
fi
readonly LABEL_SUFFIX

readonly TIMESTAMP="$(date -u +%Y-%m-%dT%H%M%SZ)"
readonly BACKUP_FILE="${BACKUP_DIR}/starter-${TIMESTAMP}${LABEL_SUFFIX}.sql.gz"

echo "[$(date -u -Iseconds)] backup starting -> ${BACKUP_FILE}"

# The inner $(cat ...) is deliberately left unquoted-in-outer-scope so it expands
# inside the container, where the secret file exists — not on the host, where it
# does not.
docker exec "${CONTAINER_NAME}" sh -c \
    "PGPASSWORD=\$(cat ${SECRET_FILE_IN_CONTAINER}) pg_dump -U ${DB_USER} -d ${DB_NAME} --no-owner --clean --if-exists" \
    | gzip -9 > "${BACKUP_FILE}"

# An empty dump is almost always a failure that pg_dump reported on stderr while
# still exiting through the pipe. Removing it matters: a 0-byte file in the
# rotation is a backup that looks present and restores nothing.
if [[ ! -s "${BACKUP_FILE}" ]]; then
    echo "FATAL: backup file is empty, removing it again." >&2
    rm -f "${BACKUP_FILE}"
    exit 2
fi

echo "[$(date -u -Iseconds)] backup done: ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

echo "[$(date -u -Iseconds)] applying retention (older than ${RETENTION_DAYS} days)"
find "${BACKUP_DIR}" -maxdepth 1 -type f -name 'starter-*.sql.gz' \
    -mtime "+${RETENTION_DAYS}" -print -delete

echo "[$(date -u -Iseconds)] current backups in ${BACKUP_DIR}:"
ls -laht "${BACKUP_DIR}" | head -20
