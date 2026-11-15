#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

image=${1:?Usage: deploy.sh IMAGE_DIGEST RELEASE_DIRECTORY}
release=${2:?Specify extracted immutable frontend release directory}
root=${SANTACUT_ROOT:-/opt/santacut}
state="$root/frontend"
[[ "$image" =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid frontend image digest'; exit 1; }
revision=${release##*/}
[[ "$revision" =~ ^[a-f0-9]{40}$ && "$release" == "$root/frontend/releases/$revision" ]] || { echo 'Invalid frontend release directory'; exit 1; }
[[ -f "$root/.env" && -f "$release/deploy/compose.yml" ]] || { echo 'Missing server environment or release'; exit 1; }

# Share the lock with backend deployments: Caddy must never be updated concurrently.
exec 9>"$root/deploy.lock"
flock -n 9 || { echo 'Another deployment is active'; exit 1; }
backend_release=$(readlink -f "$root/current")
backend_image=$(cat "$root/current-image")
[[ "$backend_image" =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid current backend image'; exit 1; }
[[ -f "$backend_release/deploy/compose.yml" && -f "$backend_release/deploy/Caddyfile" ]] || { echo 'Deploy the backend first'; exit 1; }
grep -q 'FRONTEND_UPSTREAM' "$backend_release/deploy/compose.yml" || { echo 'Deploy the backend release with frontend routing support first'; exit 1; }
grep -q 'FRONTEND_UPSTREAM' "$backend_release/deploy/Caddyfile" || { echo 'Backend Caddyfile does not support frontend routing'; exit 1; }
docker network inspect santacut_default >/dev/null

domain=$(sed -n 's/^DOMAIN=//p' "$root/.env" | tail -n 1)
[[ "$domain" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ ]] || { echo 'DOMAIN must be an unquoted hostname'; exit 1; }
mkdir -p "$state"
previous_release=$(readlink -f "$state/current" 2>/dev/null || true)
previous_image=$(cat "$state/current-image" 2>/dev/null || true)
environment_backup=$(mktemp "$state/backend-env.XXXXXX")
cp "$root/.env" "$environment_backup"
routing_changed=false

compose() { FRONTEND_IMAGE="$image" docker compose -f "$release/deploy/compose.yml" "$@"; }
backend_caddy() { APP_IMAGE="$backend_image" docker compose --env-file "$root/.env" -f "$backend_release/deploy/compose.yml" up -d --no-deps --force-recreate --wait --wait-timeout 60 caddy; }
cleanup() { rm -f "$environment_backup"; }
trap cleanup EXIT
compose config --quiet
compose pull frontend

rollback() {
  trap - ERR
  echo 'Frontend deployment failed; restoring prior frontend and Caddy routing.' >&2
  if [[ -n "$previous_release" && -n "$previous_image" ]]; then
    FRONTEND_IMAGE="$previous_image" docker compose -f "$previous_release/deploy/compose.yml" up -d --wait --wait-timeout 120 frontend || echo 'FRONTEND ROLLBACK FAILED: inspect the server' >&2
  else
    compose down || true
  fi
  if [[ "$routing_changed" == true ]]; then
    cp "$environment_backup" "$root/.env.rollback"
    mv "$root/.env.rollback" "$root/.env"
    backend_caddy || echo 'CADDY ROLLBACK FAILED: inspect the server' >&2
  fi
  exit 1
}
trap rollback ERR
compose up -d --wait --wait-timeout 180 frontend
curl --fail --silent --max-time 10 http://127.0.0.1:3000/ >/dev/null

# Preserve all other backend settings. Future backend releases keep this route.
awk '!/^FRONTEND_UPSTREAM=/' "$root/.env" > "$root/.env.next"
printf '\nFRONTEND_UPSTREAM=frontend:3000\n' >> "$root/.env.next"
mv "$root/.env.next" "$root/.env"
routing_changed=true
backend_caddy
curl --fail --silent --retry 5 --retry-delay 2 --retry-all-errors --max-time 15 "https://$domain/" >/dev/null

printf '%s\n' "$image" > "$state/current-image.next"
mv "$state/current-image.next" "$state/current-image"
ln -sfn "$release" "$state/current.next"
mv -Tf "$state/current.next" "$state/current"
trap - ERR
echo 'Frontend and public route are healthy. Current frontend release updated.'
