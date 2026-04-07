#!/bin/bash
set -e

echo "=== Building ingest-service ==="
cd ingest-service && ./gradlew clean build -x test && cd ..

echo "=== Building processing-service ==="
cd processing-service && ./gradlew clean build -x test && cd ..

echo "=== Building report-service ==="
cd report-service && ./gradlew clean build -x test && cd ..

echo "=== Starting Docker Compose ==="
docker compose up --build -d