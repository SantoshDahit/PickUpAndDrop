#!/usr/bin/env bash
# Pickup&Drop — one-shot VPS setup (testing phase: MySQL + API + TLS via sslip.io).
# Idempotent: safe to re-run. Run as a sudo-capable user on Ubuntu/Debian.
#
#   ./server-setup.sh <PUBLIC_IP>
#
# Expects the repo at /opt/pickupdrop/src and secrets at /opt/pickupdrop/.env
# (copy deploy/.env.example and fill it in first).
set -euo pipefail

IP="${1:?usage: ./server-setup.sh <PUBLIC_IP>}"
APP_DIR=/opt/pickupdrop
SRC="$APP_DIR/src"
API_HOST="api.${IP}.sslip.io"

echo "== [1/6] firewall (22/80/443 only) =="
sudo apt-get update -qq
sudo apt-get install -y -qq ufw >/dev/null
sudo ufw allow 22/tcp >/dev/null
sudo ufw allow 80/tcp >/dev/null
sudo ufw allow 443/tcp >/dev/null
sudo ufw --force enable >/dev/null
sudo ufw status | head -6

echo "== [2/6] docker + nginx + certbot =="
command -v docker >/dev/null || curl -fsSL https://get.docker.com | sudo sh
sudo apt-get install -y -qq nginx certbot python3-certbot-nginx >/dev/null
sudo usermod -aG docker "$USER" || true

echo "== [3/6] sanity: repo + secrets =="
test -f "$SRC/deploy/docker-compose.prod.yml" || { echo "repo missing at $SRC"; exit 1; }
test -f "$APP_DIR/.env" || { echo "secrets missing at $APP_DIR/.env (copy deploy/.env.example)"; exit 1; }
grep -q CHANGE_ME "$APP_DIR/.env" && { echo "fill in the CHANGE_ME values in $APP_DIR/.env"; exit 1; }

echo "== [4/6] build API image =="
test -f "$SRC/springboot/build/libs/pickupdrop-0.0.1-SNAPSHOT.jar" \
  || { echo "jar missing — rsync it or build with ./gradlew bootJar"; exit 1; }
sudo docker build -q -t pickupdrop-api "$SRC/springboot"

echo "== [5/6] start mysql + api =="
sudo docker compose -f "$SRC/deploy/docker-compose.prod.yml" --env-file "$APP_DIR/.env" up -d mysql api
for i in $(seq 1 60); do
  curl -sf -o /dev/null http://127.0.0.1:8080/v1/routes && break
  sleep 2
done
curl -sf http://127.0.0.1:8080/v1/routes >/dev/null && echo "API is up" || { sudo docker logs pickupdrop-api | tail -30; exit 1; }

echo "== [6/6] nginx vhost + TLS for ${API_HOST} =="
sudo sed "s/1\.2\.3\.4/${IP}/g" "$SRC/deploy/nginx/api-testing.sslip.conf" \
  | sudo tee "/etc/nginx/sites-available/${API_HOST}.conf" >/dev/null
sudo ln -sf "/etc/nginx/sites-available/${API_HOST}.conf" "/etc/nginx/sites-enabled/${API_HOST}.conf"
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx --non-interactive --agree-tos --register-unsafely-without-email -d "$API_HOST" \
  || echo "certbot failed (sslip.io rate limits?) — API still reachable on http://${API_HOST}"

echo
echo "DONE → https://${API_HOST}/v1/routes"
