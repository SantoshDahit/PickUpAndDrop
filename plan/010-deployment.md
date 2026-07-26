# 010 — Production deployment: getpickupdrop.com on the owner's VPS

**Status:** Draft — artifacts committed; awaiting domain purchase + server details
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
