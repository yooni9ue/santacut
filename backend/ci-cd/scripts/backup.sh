#!/usr/bin/env bash
# 실행 중인 앱 컨테이너에서 SQLite를 .backup으로 떠서 S3(SSE-KMS)에 올리고, 올라간 내용을 내려받아 대조한다.
# 암호화는 S3가 환경 KMS 키로 한다(BACKUP_REMOTE의 server_side_encryption, sse_kms_key_id).
# 컨테이너의 /tmp는 64MB tmpfs라 DB가 커지면 .backup이 실패한다. 복사본은 데이터 볼륨(/app/data/backup-tmp)에 뜨고 바로 지운다.
set -Eeuo pipefail
umask 077
root=/opt/santacut
env_file=/run/santacut/app.env
local_retention_minutes=2880
[[ -f "$root/backup.env" ]] && source "$root/backup.env"
: "${BACKUP_REMOTE:?Set rclone S3 remote with SSE-KMS in $root/backup.env}"
[[ -f "$env_file" ]] || { echo "Missing $env_file"; exit 1; }
release=$(readlink -f "$root/current")
image=$(cat "$root/current-image")
backup_dir="$root/backups"
mkdir -p "$backup_dir"
exec 8>"$root/backup.lock"
flock -n 8 || { echo 'Another backup is active'; exit 1; }
stamp=$(date -u +%Y%m%dT%H%M%SZ)
name="santacut-$stamp.sqlite"
file="$backup_dir/$name"
compose=(docker compose --env-file "$env_file" -f "$release/deploy/compose.yml")
app_tmp_dir=/app/data/backup-tmp
app_copy="$app_tmp_dir/santacut-backup.db"
app_exec() { APP_IMAGE="$image" "${compose[@]}" exec -T app "$@"; }
cleanup() {
  rm -f "${file}.partial"
  app_exec rm -rf "$app_tmp_dir" >/dev/null 2>&1 || true
}
trap cleanup EXIT
app_exec mkdir -p "$app_tmp_dir"
app_exec sqlite3 /app/data/santacut.db ".backup '$app_copy'"
app_exec cat "$app_copy" > "${file}.partial"
app_exec rm -rf "$app_tmp_dir"
[[ "$(sqlite3 "${file}.partial" 'PRAGMA integrity_check;')" == ok ]] || { echo 'Backup integrity check failed'; exit 1; }
chmod 600 "${file}.partial"
mv "${file}.partial" "$file"
rclone copyto "$file" "${BACKUP_REMOTE%/}/$name"
# SSE-KMS 객체의 ETag는 MD5가 아니라서 해시 대신 내려받아 바이트 단위로 대조한다.
rclone check "$backup_dir" "${BACKUP_REMOTE%/}" --one-way --download --include "$name"
find "$backup_dir" -maxdepth 1 -type f \( -name 'santacut-*.sqlite' -o -name 'santacut-*.sqlite.age' \) \
  -mmin +"$local_retention_minutes" -delete
echo "Off-host backup verified: $name"
