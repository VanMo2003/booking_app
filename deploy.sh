#!/usr/bin/env bash
set -euo pipefail

APP_NAME="booking-app"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  echo "Docker Compose is not installed."
  exit 1
fi

echo "Using compose command: ${COMPOSE_CMD}"
if [[ -f ".env" ]]; then
  echo "Found .env file. Compose will use your overrides from .env."
else
  echo "No .env file found. Using defaults from docker-compose.yml."
fi
${COMPOSE_CMD} down
${COMPOSE_CMD} up -d --build

echo "Deployment completed."
echo "Check status: ${COMPOSE_CMD} ps"
echo "Check logs:   ${COMPOSE_CMD} logs -f ${APP_NAME}"
