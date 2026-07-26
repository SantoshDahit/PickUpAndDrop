#!/usr/bin/env bash
# Pickup&Drop — one-time server prep on infrastructure that already runs
# `shared-mysql` (MySQL 8) and `caddy` (80/443 + auto TLS).
# Idempotent. Run as the deploy user:
#
#   ./server-setup.sh <PUBLIC_IP> <DOCKER_NETWORK> <CADDYFILE_PATH>
#
# It does NOT create a database — run the SQL printed at the end in
# shared-mysql (needs the MySQL root password, which stays with the owner).
set -euo pipefail

IP="${1:?usage: ./server-setup.sh <PUBLIC_IP> <DOCKER_NETWORK> <CADDYFILE_PATH>}"
NET="${2:?docker network that shared-mysql is on}"
CADDYFILE="${3:?path to the Caddyfile on the host}"
APP_DIR=/opt/pickupdrop
API_HOST="api.${IP}.sslip.io"
BLUE_PORT=18080

echo "== [1/4] app dir + secrets =="
sudo mkdir -p "$APP_DIR"
sudo chown "$USER" "$APP_DIR"
test -f "$APP_DIR/.env" || { echo "put your filled .env at $APP_DIR/.env first (see deploy/.env.example)"; exit 1; }
grep -q CHANGE_ME "$APP_DIR/.env" && { echo "fill in the CHANGE_ME values in $APP_DIR/.env"; exit 1; }
chmod 600 "$APP_DIR/.env"

echo "== [2/4] sanity: shared infra reachable =="
docker network inspect "$NET" >/dev/null || { echo "network $NET not found"; exit 1; }
docker ps --format '{{.Names}}' | grep -q '^shared-mysql$' || { echo "shared-mysql not running"; exit 1; }
docker ps --format '{{.Names}}' | grep -q '^caddy$' || { echo "caddy not running"; exit 1; }

echo "== [3/4] Caddy site for ${API_HOST} =="
if ! grep -q "$API_HOST" "$CADDYFILE"; then
  printf '\n%s {\n\treverse_proxy 127.0.0.1:%s\n}\n' "$API_HOST" "$BLUE_PORT" | sudo tee -a "$CADDYFILE" >/dev/null
  docker exec caddy caddy reload --config /etc/caddy/Caddyfile
  echo "added + reloaded"
else
  echo "already present"
fi

echo "== [4/4] database bootstrap SQL (run in shared-mysql as root) =="
DB_PASS=$(grep '^DB_PASSWORD=' "$APP_DIR/.env" | cut -d= -f2-)
cat <<SQL
CREATE DATABASE IF NOT EXISTS pickupdrop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'pickupdrop'@'%' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON pickupdrop.* TO 'pickupdrop'@'%';
FLUSH PRIVILEGES;
SQL
echo
echo "then push to main (or Run workflow) → https://${API_HOST}/v1/routes"
