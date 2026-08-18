#!/bin/bash
# Generates the Docker secrets this stack needs, once, on the deployment host.
#
# Idempotent by design: an existing file is never overwritten. Re-running after a
# partial failure, or after adding a new secret to the list, does the right thing —
# and cannot silently rotate a password that other components already hold.
#
#   ./setup-secrets.sh          generate what is missing
#   ./setup-secrets.sh show     print the values (for a password manager)
#
# Secrets that are *not* generated here: keycloak_client_secret. That one is
# minted by Keycloak as part of its realm import, so this script only creates a
# placeholder and tells you to replace it. Generating a random value would produce
# a stack that starts cleanly and fails every login.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SECRETS_DIR="$SCRIPT_DIR/secrets"

# --- Permissions -------------------------------------------------------------
# Directory 0700, files 0644 — and the file mode is the counter-intuitive half.
#
# Compose outside Swarm bind-mounts a file-based secret and preserves its host
# mode. The container runs as a non-root UID that has nothing to do with the host
# owner, so 0600 makes the file unreadable inside the container. The failure is
# not a permission error either: the application boots and then reports an
# authentication failure against Postgres or Keycloak, which sends you looking in
# entirely the wrong place.
#
# 0644 inside a 0700 directory is not the compromise it looks like: another user
# on the host cannot traverse the directory, so it cannot reach the files
# regardless of their own mode.
DIR_MODE=700
FILE_MODE=644

# --- What to generate --------------------------------------------------------
# name:kind — base64 for passwords, hex where a value ends up in a URL or an
# access-key position and should stay free of characters that need escaping.
SECRETS=(
    "postgres_password:base64"
    "db_password:base64"
    "redis_password:base64"
    "s3_access_key:hex"
    "s3_secret_key:base64"
    "download_token_secret:base64"
    "grafana_admin_password:base64"
)

PLACEHOLDERS=(
    "keycloak_client_secret"
)

generate() {
    case "$1" in
        base64) openssl rand -base64 32 | tr -d '\n' ;;
        hex) openssl rand -hex 16 | tr -d '\n' ;;
        *) echo "unknown secret kind: $1" >&2; exit 1 ;;
    esac
}

show() {
    if [ ! -d "$SECRETS_DIR" ]; then
        echo "No secrets directory yet — run without arguments first." >&2
        exit 1
    fi
    for file in "$SECRETS_DIR"/*.txt; do
        printf '%-28s %s\n' "$(basename "$file" .txt)" "$(cat "$file")"
    done
}

if [ "${1:-generate}" = "show" ]; then
    show
    exit 0
fi

mkdir -p "$SECRETS_DIR"
chmod "$DIR_MODE" "$SECRETS_DIR"

created=0
for entry in "${SECRETS[@]}"; do
    name="${entry%%:*}"
    kind="${entry##*:}"
    file="$SECRETS_DIR/$name.txt"

    if [ -f "$file" ]; then
        echo "  = $name (exists, left alone)"
        continue
    fi
    # No trailing newline: every consumer would have to strip it, and the one that
    # forgets produces an authentication failure that looks like a wrong password.
    generate "$kind" > "$file"
    echo "  + $name (generated)"
    created=$((created + 1))
done

for name in "${PLACEHOLDERS[@]}"; do
    file="$SECRETS_DIR/$name.txt"
    if [ -f "$file" ]; then
        echo "  = $name (exists, left alone)"
        continue
    fi
    printf 'REPLACE_ME' > "$file"
    echo "  ! $name (placeholder — copy the real value from Keycloak)"
done

chmod "$FILE_MODE" "$SECRETS_DIR"/*.txt

echo
echo "Secrets directory: $SECRETS_DIR"
echo "Permissions: dir 0$DIR_MODE (host-owner only), files 0$FILE_MODE (readable by the container UID)."
echo "$created new secret(s) generated."
echo
echo "Next:"
echo "  1. Replace secrets/keycloak_client_secret.txt with the value from your Keycloak realm."
echo "  2. Store the values somewhere safe:  ./setup-secrets.sh show"
echo "  3. cp .env.example .env  and set DOMAIN / AUTH_DOMAIN."
