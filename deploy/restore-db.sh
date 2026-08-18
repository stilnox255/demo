#!/bin/bash
# Restores the application database from a backup written by backup-db.sh.
#
# Manual only, never from cron.
#
#   ./restore-db.sh                                   list what is available
#   ./restore-db.sh /srv/starter/backups/starter-….sql.gz
#
# Takes a pre-restore snapshot first, unconditionally. "I restored the wrong
# backup" has to stay recoverable, and the moment you need this script is the
# moment you are least likely to think of it yourself.
#
# The application keeps running during the restore. That is deliberate for a
# starter: stopping it would need orchestration this script cannot assume. On a
# real system, scale the backend to zero first — the dump's --clean drops tables
# out from under any open connection.
set -euo pipefail

readonly BACKUP_DIR="${STARTER_BACKUP_DIR:-/srv/starter/backups}"
readonly CONTAINER_NAME="${STARTER_POSTGRES_CONTAINER:-starter-postgres-1}"
readonly DB_NAME="${STARTER_DB_NAME:-starter}"
readonly DB_USER="${STARTER_DB_USER:-starter}"
readonly SECRET_FILE_IN_CONTAINER="${STARTER_DB_SECRET_FILE:-/run/secrets/db_password}"
readonly SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <backup-file.sql.gz>"
    echo
    echo "Available backups in ${BACKUP_DIR}:"
    ls -laht "${BACKUP_DIR}"/starter-*.sql.gz 2>/dev/null || echo "  (none)"
    exit 1
fi

readonly BACKUP_FILE="$1"

if [[ ! -r "${BACKUP_FILE}" ]]; then
    echo "FATAL: cannot read ${BACKUP_FILE}" >&2
    exit 1
fi

if ! gzip -t "${BACKUP_FILE}" 2>/dev/null; then
    echo "FATAL: ${BACKUP_FILE} is not a valid gzip file — refusing to restore from it." >&2
    exit 2
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "FATAL: postgres container '${CONTAINER_NAME}' is not running." >&2
    exit 1
fi

echo "About to restore ${BACKUP_FILE} into database '${DB_NAME}'."
echo "This drops and recreates every object in the dump."
read -r -p "Type the database name to confirm: " confirmation
if [[ "${confirmation}" != "${DB_NAME}" ]]; then
    echo "Aborted."
    exit 3
fi

echo "[$(date -u -Iseconds)] taking a pre-restore snapshot first"
"${SCRIPT_DIR}/backup-db.sh" pre-restore

echo "[$(date -u -Iseconds)] restoring"
# ON_ERROR_STOP so a broken dump fails here instead of leaving a half-restored
# database that looks like it worked.
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" sh -c \
    "PGPASSWORD=\$(cat ${SECRET_FILE_IN_CONTAINER}) psql -v ON_ERROR_STOP=1 -U ${DB_USER} -d ${DB_NAME}"

echo "[$(date -u -Iseconds)] restore complete."
echo "Restart the backend so it reconnects and re-runs its Flyway check:"
echo "  docker compose restart backend"
