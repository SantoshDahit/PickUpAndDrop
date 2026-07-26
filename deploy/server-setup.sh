#!/usr/bin/env bash
# Pickup&Drop — one-time server prep on infrastructure that already runs
# `shared-mysql` (MySQL 8) and `caddy` (80/443 + auto TLS).
# Idempotent. Run as the deploy user:
#
#   ./server-setup.sh 217.15.165.238 restaurant-network /home/deploy/Caddyfile
#
# No sudo needed: the deploy user is in the docker group and owns the Caddyfile.
#
# Runtime secrets are NOT stored here — the deploy workflow injects them from
# GitHub Actions secrets. This script only prepares Caddy + prints the DB SQL
# (run it in shared-mysql; the MySQL root password stays with the owner).
set -euo pipefail

IP="${1:?usage: ./server-setup.sh <PUBLIC_IP> <DOCKER_NETWORK> <CADDYFILE_PATH>}"
NET="${2:?docker network that shared-mysql is on}"
CADDYFILE="${3:?path to the Caddyfile on the host}"
API_HOST="api.${IP}.sslip.io"
BLUE_PORT=18080

echo "== [1/3] sanity: shared infra reachable =="
docker network inspect "$NET" >/dev/null || { echo "network $NET not found"; exit 1; }
docker ps --format '{{.Names}}' | grep -q '^shared-mysql$' || { echo "shared-mysql not running"; exit 1; }
docker ps --format '{{.Names}}' | grep -q '^caddy$' || { echo "caddy not running"; exit 1; }

echo "== [2/3] Caddy site for ${API_HOST} =="
if ! grep -q "$API_HOST" "$CADDYFILE"; then
  printf '\n%s {\n\treverse_proxy 127.0.0.1:%s\n}\n' "$API_HOST" "$BLUE_PORT" >> "$CADDYFILE"
  docker exec caddy caddy reload --config /etc/caddy/Caddyfile
  echo "added + reloaded"
else
  echo "already present"
fi

echo "== [3/3] database bootstrap SQL (run in shared-mysql as root) =="
cat <<'SQL'
CREATE DATABASE IF NOT EXISTS pickupdrop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'pickupdrop'@'%' IDENTIFIED BY '<the DB_PASSWORD GitHub secret>';
GRANT ALL PRIVILEGES ON pickupdrop.* TO 'pickupdrop'@'%';
FLUSH PRIVILEGES;
SQL
echo
echo "then push to main (or Run workflow) → https://${API_HOST}/v1/routes"
