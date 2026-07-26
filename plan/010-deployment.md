# 010 — Production deployment: getpickupdrop.com on the owner's VPS

**Status:** Draft — **testing phase first (owner, 2026-07-26): Contabo VPS + Vercel**, domain purchase deferred

## 0-a. Server reality check (2026-07-26) — reuse existing shared infra

The Contabo box already runs the owner's other project and shares infrastructure.
The deployment adapts rather than duplicating:

| Existing | Used for Pickup&Drop |
|---|---|
| `shared-mysql` (MySQL 8, holds `restaurant_booking_system`) | Add a `pickupdrop` database + user — **no second MySQL container** |
| `caddy` (owns 80/443, automatic TLS) | Reverse proxy + certificates — **Nginx and certbot dropped**; Blue-Green flip is a Caddyfile edit + `caddy reload` |
| Docker Hub `santosh390`, tags `…-api:dev.YYYY.MM.DD` | Same convention-18 tag scheme, image `santosh390/pickupdrop-api` |

Host ports for Blue/Green are **18080/18081** so they cannot collide with the
other project's containers. MySQL root stays with the owner: the setup script
prints the `CREATE DATABASE/USER/GRANT` SQL to run manually rather than
demanding root credentials.

Two workflow inputs come from GitHub **Variables** (not secrets — they are not
sensitive): `DOCKER_NETWORK` (the network `shared-mysql` is on) and `CADDYFILE`
(host path of the Caddyfile).

## 0-b. Testing phase — Contabo VPS + Vercel

- **Contabo VPS**: API container only (Blue-Green via `docker run`, convention 18), joined to the
  shared network so it reaches `shared-mysql`. Public hostname without buying a domain:
  **`api.<IP>.sslip.io`** (sslip.io resolves any embedded IP), TLS issued automatically by the
  existing Caddy — required because the admin SPA calls the API from the browser (HTTPS→HTTPS).
- **Vercel** (free tier): two projects from the same repo —
  root `/` = Next.js customer app (env `API_URL=https://api.<IP>.sslip.io`, server-side calls);
  root `admin-web/` = Vite admin console (env `VITE_API_URL=...`, `vercel.json` SPA rewrite added).
- API `CORS_ALLOWED_ORIGINS` = the admin Vercel URL (customer app needs none — server-side).
- Graduation to production (§1 below, getpickupdrop.com) is a DNS + env change, no re-architecture.
**Follows:** `springboot/conventions/18-deployment-convention.md` (Docker + Nginx; Blue-Green via GitHub Actions as Phase 2)

## 1. Target architecture

One VPS (SSH root), Docker Compose + host Nginx + Let's Encrypt:

| DNS record | Serves | Backed by |
|---|---|---|
| `getpickupdrop.com`, `www.` → A → VPS IP | Customer Next.js app | `pickupdrop-web` container :3000 (loopback) |
| `api.getpickupdrop.com` → A → VPS IP | Spring Boot API | `pickupdrop-api` container :8080 (loopback) |
| `admin.getpickupdrop.com` → A → VPS IP | Admin console | Static files at `/var/www/pickupdrop-admin` |

MySQL runs as a compose service with a named volume — never exposed publicly. All app ports
bind to 127.0.0.1; only Nginx (80/443) faces the internet.

## 2. Artifacts (this repo)

- `springboot/Dockerfile` — convention 18 image, Temurin 21 (project toolchain)
- `Dockerfile.web` — Next.js standalone build (`output: "standalone"` added to next.config)
- `deploy/docker-compose.prod.yml` — mysql + api + web, healthcheck-ordered, loopback ports
- `deploy/nginx/*.conf` — the three vhosts (api proxied on the Blue port, per convention)
- `deploy/.env.example` — the four secrets (`openssl rand -base64 48`)
- Admin SPA is built with `VITE_API_URL=https://api.getpickupdrop.com npm run build` and rsynced

## 3. Phases

**Phase 1 (this plan): manual first deploy** — buy domain, point DNS, install Docker+Nginx+certbot,
build images on the server, `docker compose up -d`, enable vhosts, issue TLS. Documented as the
runbook in the final guide; repeatable by hand.

**Phase 2 (next): convention 18 in full** — GitHub Actions workflow, Docker Hub pushes, Blue/Green
containers (`pickupdrop-api-pro-BLUE/GREEN`), port-flip in the api vhost, zero-downtime. Requires
the repo on GitHub (push pending owner approval) + Docker Hub account + repo secrets (SSH key,
host, Docker Hub token).

## 4. Production checklist

- [ ] Secrets set in `/opt/pickupdrop/.env` — JWT_SECRET random ≥43 chars, strong DB + seed-admin
      passwords (the app logs a warning if the default admin password survives)
- [ ] `CORS_ALLOWED_ORIGINS=https://admin.getpickupdrop.com` (customer app calls server-side — no CORS needed)
- [ ] MySQL not port-mapped to the host; app ports loopback-only
- [ ] TLS on all three hosts, HTTP→HTTPS redirect (certbot handles)
- [ ] Firewall: allow 22/80/443 only
- [ ] Backups: nightly `mysqldump` to off-server storage (simple cron to start)
- [ ] Seed dev users exist in migrations — fine for launch (real emails onboard organically);
      rotate `SEED_ADMIN_PASSWORD` before sharing the admin URL
