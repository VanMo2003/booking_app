# Deploy booking_app on EC2 with Docker (Port 80)

## 1) Open Security Group
- Inbound `22` from your IP
- Inbound `80` from `0.0.0.0/0`

## 2) Install Docker on EC2 (Ubuntu)
```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
newgrp docker
```

## 3) Deploy immediately (app + mysql)
```bash
docker compose up -d --build
```

## 4) Optional: customize config via .env
```bash
cp .env.example .env
nano .env
docker compose down
docker compose up -d --build
```

## 5) Verify
```bash
docker compose ps
docker compose logs -f booking-app
docker compose logs -f booking-db
curl http://localhost/booking-app/api/v1
```
