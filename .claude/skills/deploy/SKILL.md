---
name: deploy
description: Guided checklist for building and deploying the booking_app Spring Boot backend via Docker Compose (local Docker or a live EC2 host). Use this whenever the user asks to deploy, redeploy, ship, push out, restart, or "run the deploy" for booking_app - locally or "on the server"/"on EC2" - even if they just say "deploy this" without more detail.
---

# Deploy booking_app

This wraps the repo's own runbook (`deploy.sh`, `docker-compose.yml`, [DEPLOY_EC2.md](../../../DEPLOY_EC2.md))
into a single checklist. It stays a *guided* checklist rather than one command to run blindly,
because `docker compose down`/`up` restarts a service that may already be live for real users, and
`docker-compose.yml` ships with demo secrets (JWT signer key, VNPay sandbox keys, DB password) baked
in as defaults — those must not reach a real host unchanged.

## 1. Confirm the target

Ask (if not already clear from context): **local Docker** on this machine, or a **live/EC2 host**?

- Local: safe to proceed directly.
- Live/EC2: this restarts whatever is currently running there. Confirm with the user before running
  any `docker compose` command against that host — don't just run it because the checklist says so.
- Brand-new EC2 instance with nothing deployed yet: first do the one-time setup in
  [DEPLOY_EC2.md](../../../DEPLOY_EC2.md) (open inbound `22`/`80` in the security group, install
  Docker + the compose plugin) — everything below assumes that's already done.

## 2. Check `.env` before deploying to a real host

`docker-compose.yml` falls back to demo values baked into the repo (`JWT_SIGNER_KEY`,
`VNPAY_TMN_CODE`/`VNPAY_SECRET_KEY`, `MYSQL_*` passwords) if `.env` is absent. That's fine for a
local throwaway run, never fine for a real deploy.

- If `.env` doesn't exist yet: `cp .env.example .env`, then tell the user which values need real
  secrets (JWT signer key, VNPay credentials, DB passwords) — don't invent or fill in secret values
  yourself, ask the user to provide them.
- If `.env` already exists: leave it alone unless the user asks you to change something in it.

## 3. Build and bring the stack up

```bash
docker compose up -d --build
```

(equivalent to running `./deploy.sh`, which does `docker compose down` then the above and prints
the same status/log hints as step 4 below). `booking-app` won't start until `booking-db`'s
healthcheck passes (`depends_on: condition: service_healthy` in `docker-compose.yml`), so there's
no need for a manual wait loop.

## 4. Verify

```bash
docker compose ps                      # both containers should show healthy/running
docker compose logs -f booking-app     # tail app logs if anything looks off
docker compose logs -f booking-db
curl http://localhost/booking-app/api/v1
```

On a remote host, hit the host's public IP/DNS on port 80 instead of `localhost` for the curl check.

## 5. First deploy against a fresh database

On first boot against an empty DB, the app auto-seeds an admin account (`admin123` / `admin123`,
see `ApplicationInitConfig`). Remind the user to log in and change that password — it's a known
default, not something to leave on a reachable host.
