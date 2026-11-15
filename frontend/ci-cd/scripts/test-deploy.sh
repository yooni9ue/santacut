#!/usr/bin/env bash
# Exercise deployment state changes with fake Docker/curl; no real server is touched.
set -Eeuo pipefail
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
export SANTACUT_ROOT="$fixture/server"
export TEST_LOG="$fixture/commands"
mkdir -p "$fixture/bin" "$SANTACUT_ROOT/frontend/releases" "$SANTACUT_ROOT/releases/backend/deploy"
export PATH="$fixture/bin:$PATH"

cat > "$fixture/bin/docker" <<'STUB'
#!/usr/bin/env bash
printf 'docker %s\n' "$*" >> "$TEST_LOG"
STUB
cat > "$fixture/bin/curl" <<'STUB'
#!/usr/bin/env bash
printf 'curl %s\n' "$*" >> "$TEST_LOG"
if [[ "${FAIL_PUBLIC:-false}" == true && "$*" == *https://* ]]; then exit 22; fi
STUB
# macOS lacks GNU flock/readlink/mv; wrappers implement only the fixture operations.
cat > "$fixture/bin/flock" <<'STUB'
#!/usr/bin/env bash
exit 0
STUB
cat > "$fixture/bin/readlink" <<'STUB'
#!/usr/bin/env bash
if [[ "$1" == -f ]]; then shift; fi
[[ -L "$1" ]] || exit 1
/usr/bin/readlink "$1"
STUB
cat > "$fixture/bin/mv" <<'STUB'
#!/usr/bin/env bash
if [[ "$1" == -Tf ]]; then shift; fi
/bin/mv -f "$@"
STUB
chmod +x "$fixture/bin/"*
revision=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
release="$SANTACUT_ROOT/frontend/releases/$revision"
mkdir -p "$release/deploy"
cp "$script_dir/../deploy/compose.yml" "$release/deploy/compose.yml"
backend_release="$SANTACUT_ROOT/releases/backend"
printf 'FRONTEND_UPSTREAM\n' > "$backend_release/deploy/compose.yml"
printf 'FRONTEND_UPSTREAM\n' > "$backend_release/deploy/Caddyfile"
ln -s "$backend_release" "$SANTACUT_ROOT/current"
backend_image="ghcr.io/example/backend@sha256:$(printf '0%.0s' {1..64})"
printf '%s\n' "$backend_image" > "$SANTACUT_ROOT/current-image"
printf 'DOMAIN=santacut.example.com\nOTHER_SETTING=keep-me\n' > "$SANTACUT_ROOT/.env"
original_environment=$(cat "$SANTACUT_ROOT/.env")
image="ghcr.io/example/frontend@sha256:$(printf '1%.0s' {1..64})"

if bash "$script_dir/deploy.sh" invalid "$release" >/dev/null 2>&1; then
  echo 'FAIL: invalid digest was accepted'; exit 1
fi
[[ ! -f "$TEST_LOG" ]] || { echo 'FAIL: invalid input reached Docker'; exit 1; }

if FAIL_PUBLIC=true bash "$script_dir/deploy.sh" "$image" "$release" >"$fixture/failed.log" 2>&1; then
  echo 'FAIL: broken public route was accepted'; exit 1
fi
[[ "$(cat "$SANTACUT_ROOT/.env")" == "$original_environment" ]]
[[ ! -e "$SANTACUT_ROOT/frontend/current-image" ]]
grep -q 'down' "$TEST_LOG"
echo 'PASS: failed first deploy restores backend routing and does not publish state'

bash "$script_dir/deploy.sh" "$image" "$release" >"$fixture/success.log" 2>&1
[[ "$(cat "$SANTACUT_ROOT/frontend/current-image")" == "$image" ]]
[[ "$(readlink "$SANTACUT_ROOT/frontend/current")" == "$release" ]]
grep -q '^FRONTEND_UPSTREAM=frontend:3000$' "$SANTACUT_ROOT/.env"
grep -q '^OTHER_SETTING=keep-me$' "$SANTACUT_ROOT/.env"
echo 'PASS: healthy deploy publishes frontend state and preserves backend settings'

next_revision=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
next_release="$SANTACUT_ROOT/frontend/releases/$next_revision"
mkdir -p "$next_release/deploy"
cp "$release/deploy/compose.yml" "$next_release/deploy/compose.yml"
next_image="ghcr.io/example/frontend@sha256:$(printf '2%.0s' {1..64})"
if FAIL_PUBLIC=true bash "$script_dir/deploy.sh" "$next_image" "$next_release" >"$fixture/rollback.log" 2>&1; then
  echo 'FAIL: failed upgrade was accepted'; exit 1
fi
[[ "$(cat "$SANTACUT_ROOT/frontend/current-image")" == "$image" ]]
[[ "$(readlink "$SANTACUT_ROOT/frontend/current")" == "$release" ]]
grep -Fq "$release/deploy/compose.yml up -d --wait --wait-timeout 120 frontend" "$TEST_LOG"
echo 'PASS: failed upgrade restores previous frontend release'
