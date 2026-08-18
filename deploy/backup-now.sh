#!/bin/bash
# Triggers a labelled backup on the deployment host over SSH, from a repo clone.
#
#   ./deploy/backup-now.sh
#   ./deploy/backup-now.sh before-migration
#   ./deploy/backup-now.sh "before config change"
#
# Why a wrapper instead of just typing the ssh command: at 03:00 during an
# incident, `backup-now before-migration` is easier to get right than
# `ssh host /srv/starter/backup-db.sh …`. It also sanitizes the label *before* it
# reaches a remote shell, which the ad-hoc version does not.
set -euo pipefail

readonly SSH_TARGET="${STARTER_SSH_TARGET:-starter}"
readonly REMOTE_SCRIPT="${STARTER_REMOTE_BACKUP_SCRIPT:-/srv/starter/backup-db.sh}"

LABEL=""
if [[ $# -ge 1 && -n "$1" ]]; then
    LABEL="$(printf %s "$1" | tr -c '[:alnum:]._-' '-')"
    LABEL="${LABEL#-}"
    LABEL="${LABEL%-}"
fi

echo "Triggering backup on ${SSH_TARGET}${LABEL:+ (label: ${LABEL})}"
# shellcheck disable=SC2029  # remote expansion is intended; LABEL is sanitized above
ssh "${SSH_TARGET}" "${REMOTE_SCRIPT} ${LABEL}"
