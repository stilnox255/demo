#!/bin/bash
# Copies deploy/ to the host and restarts the stack there.
#
#   ./deploy.sh root@example.com        rolling update (pull + up -d)
#   ./deploy.sh -f root@example.com     full restart (down, then pull + up -d)
#
# Restartable on purpose: every step is idempotent, so re-running after a failed
# transfer or a half-finished restart converges instead of compounding. There is
# no build step here — CI builds and pushes the images, and this only moves tags
# (ADR-38). A deploy that builds is a deploy whose artifact was never tested.
set -euo pipefail

REMOTE_PATH="${STARTER_REMOTE_PATH:-/srv/starter}"
FULL_RESTART=false

usage() {
    echo "Usage: $(basename "$0") [-f] <user@host>"
    echo
    echo "  -f    full restart (docker compose down before pull + up)"
    echo
    echo "Examples:"
    echo "  $(basename "$0") root@example.com"
    echo "  $(basename "$0") -f root@example.com"
    exit 1
}

while getopts "f" opt; do
    case "$opt" in
        f) FULL_RESTART=true ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))

if [ "$#" -ne 1 ]; then
    usage
fi

REMOTE="$1"

echo "Deploying to $REMOTE:$REMOTE_PATH"

# --delete keeps the remote copy honest, so a file removed here disappears there
# too. secrets/ and .env are excluded because they are generated on the host and
# only exist there — syncing would delete them.
echo "Copying deploy/ ..."
rsync -av --delete \
    --exclude=".env" \
    --exclude=".env.example" \
    --exclude="secrets/" \
    deploy/ "$REMOTE:$REMOTE_PATH/"

if [ "$FULL_RESTART" = true ]; then
    echo "Full restart ..."
    ssh "$REMOTE" "cd $REMOTE_PATH && docker compose down && docker compose pull && docker compose up -d"
else
    # Rolling by default: compose only recreates containers whose image or config
    # actually changed, so an unchanged database stays up.
    echo "Rolling update ..."
    ssh "$REMOTE" "cd $REMOTE_PATH && docker compose pull && docker compose up -d"
fi

echo "Waiting for the backend to report ready ..."
ssh "$REMOTE" "cd $REMOTE_PATH && for i in \$(seq 1 30); do
    if docker compose ps backend --format '{{.Health}}' | grep -q healthy; then
        echo 'backend healthy'; exit 0
    fi
    sleep 5
done
echo 'backend did not become healthy in 150s — check: docker compose logs backend' >&2
exit 1"

echo "Deployment complete."
