#!/bin/sh
# Same job as backend-entrypoint.sh: secret files become environment variables,
# with the trailing newline stripped.
set -eu

export RUSTFS_ACCESS_KEY="$(cat /run/secrets/s3_access_key)"
export RUSTFS_SECRET_KEY="$(cat /run/secrets/s3_secret_key)"

exec /usr/bin/rustfs /data
