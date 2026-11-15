#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
root=/opt/santacut
[[ -f "$root/backup.env" ]] && source "$root/backup.env"
: "${BACKUP_AGE_RECIPIENT:?Set public age recipient}"
: "${BACKUP_REMOTE:?Set rclone remote:bucket/path}"
release=$(readlink -f "$root/current")
image=$(cat "$root/current-image")
backup_dir="$root/backups"
mkdir -p "$backup_dir"
exec 8>"$root/backup.lock"
flock -n 8 || { echo 'Another backup is active'; exit 1; }
stamp=$(date -u +%Y%m%dT%H%M%SZ)
file="$backup_dir/santacut-$stamp.sqlite.age"
compose=(docker compose --env-file "$root/.env" -f "$release/deploy/compose.yml")
cleanup() {
  rm -f "${file}.partial"
  APP_IMAGE="$image" "${compose[@]}" exec -T app rm -f /tmp/santacut-backup.db >/dev/null 2>&1 || true
}
trap cleanup EXIT
APP_IMAGE="$image" "${compose[@]}" exec -T app sqlite3 /app/data/santacut.db ".backup '/tmp/santacut-backup.db'"
APP_IMAGE="$image" "${compose[@]}" exec -T app cat /tmp/santacut-backup.db |
  age -r "$BACKUP_AGE_RECIPIENT" -o "${file}.partial"
APP_IMAGE="$image" "${compose[@]}" exec -T app rm -f /tmp/santacut-backup.db
mv "${file}.partial" "$file"
rclone copyto "$file" "${BACKUP_REMOTE%/}/$(basename "$file")"
rclone check "$backup_dir" "$BACKUP_REMOTE" --one-way --include "$(basename "$file")"
find "$backup_dir" -maxdepth 1 -type f -name 'santacut-*.sqlite.age' -mtime +7 -delete
echo "Encrypted off-host backup verified: $(basename "$file")"
