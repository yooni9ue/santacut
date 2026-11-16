#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

image=${1:?Usage: deploy.sh IMAGE_DIGEST RELEASE_DIRECTORY}
release=${2:?Specify extracted immutable release directory}
root=/opt/santacut
# 앱 설정은 부팅 때 S3에서 받은 tmpfs 파일이다(santacut-infra docs/operations.md 6절).
env_file=/run/santacut/app.env
[[ "$image" =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid image digest'; exit 1; }
[[ "$release" =~ ^/opt/santacut/releases/[a-f0-9]{40}$ ]] || { echo 'Invalid release directory'; exit 1; }
[[ -f "$env_file" && -f "$release/deploy/compose.yml" ]] || { echo 'Missing environment or release'; exit 1; }
exec 9>"$root/deploy.lock"
flock -n 9 || { echo 'Another deployment is active'; exit 1; }

previous_release=$(readlink -f "$root/current" 2>/dev/null || true)
previous_image=$(cat "$root/current-image" 2>/dev/null || true)
compose() { APP_IMAGE="$image" docker compose --env-file "$env_file" -f "$release/deploy/compose.yml" "$@"; }
compose config --quiet
compose pull app

# Capture a restore point before an application update can change the SQLite schema.
if [[ -n "$previous_release" && -n "$previous_image" ]]; then
  [[ -f "$root/backup.env" ]] || { echo 'Create /opt/santacut/backup.env with BACKUP_REMOTE before upgrading'; exit 1; }
  backup_script="$root/backup.sh"
  [[ -f "$backup_script" ]] || { echo 'Install scripts/backup.sh as /opt/santacut/backup.sh first'; exit 1; }
  bash "$backup_script"
fi

rollback() {
  trap - ERR
  echo 'Deployment failed; attempting previous application image. Database is not rolled back.' >&2
  if [[ -n "$previous_release" && -n "$previous_image" ]]; then
    APP_IMAGE="$previous_image" docker compose --env-file "$env_file" -f "$previous_release/deploy/compose.yml" up -d --wait --wait-timeout 360 app caddy || echo 'ROLLBACK FAILED: operator intervention required' >&2
  else
    echo 'No prior release: inspect logs and fix configuration.' >&2
  fi
  exit 1
}
trap rollback ERR
compose up -d --wait --wait-timeout 360 app caddy
curl --fail --silent --max-time 10 http://127.0.0.1:8080/actuator/health/readiness >/dev/null
printf '%s\n' "$image" > "$root/current-image.next"
mv "$root/current-image.next" "$root/current-image"
ln -sfn "$release" "$root/current.next"
mv -Tf "$root/current.next" "$root/current"
trap - ERR
echo 'Deployment healthy. Current release updated.'
