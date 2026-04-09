#!/bin/bash
set -e

echo "=== Checking host configuration ==="
if ! grep -q "auth.files-pipeline.local" /etc/hosts; then
  echo ""
  echo "⚠️  Please add the following entry to your /etc/hosts file:"
  echo ""
  echo "  127.0.0.1 auth.files-pipeline.local"
  echo ""
  echo "Run: echo '127.0.0.1 auth.files-pipeline.local' | sudo tee -a /etc/hosts"
  exit 1
fi

echo "=== Building ingest-service ==="
cd ingest-service && ./gradlew clean build -x test && cd ..

echo "=== Building processing-service ==="
cd processing-service && ./gradlew clean build -x test && cd ..

echo "=== Building report-service ==="
cd report-service && ./gradlew clean build -x test && cd ..

echo "=== Starting Docker Compose ==="
docker compose up --build -d