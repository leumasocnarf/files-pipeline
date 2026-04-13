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

echo "=== Starting infrastructure ==="
docker compose up -d postgres kafka schema-registry keycloak api-gateway

echo "=== Waiting for Schema Registry ==="
until curl -sf http://localhost:8085/config > /dev/null 2>&1; do
  echo "Waiting for Schema Registry..."
  sleep 2
done
echo "Schema Registry ready"

echo "=== Configuring Schema Registry ==="
curl -s -X PUT http://localhost:8085/config \
  -H "Content-Type: application/json" \
  -d '{"compatibility": "BACKWARD"}' > /dev/null
echo "Schema Registry configured with BACKWARD compatibility"

echo "=== Starting application services ==="
docker compose up -d ingest-service processing-service report-service

echo "=== Done ==="