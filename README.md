# files-pipeline

An asynchronous file processing pipeline built with **Kotlin**, **Spring Boot 4**, **Apache Kafka**, and **PostgreSQL**.
Files are ingested, validated, queued, and processed in scheduled batches across three independent microservices.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Tech Stack](#tech-stack)
- [Architecture Decisions](#architecture-decisions)
- [Architecture Diagram](#architecture-diagram)
- [Access Points](#access-points)
- [Authentication](#authentication)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Running Tests](#running-tests)

## Features

- **Multi-format file ingestion** — supports CSV, JSON, and XML with per-format validation
- **Asynchronous batch processing** — files are queued and processed on a configurable schedule
- **Event-driven architecture** — services communicate through Kafka topics, not direct HTTP calls
- **Rich aggregation reports** — revenue totals, category breakdowns, percentage distributions, date range analysis
- **Database per service** — each microservice owns its data independently
- **Atomic job claiming** — `FOR UPDATE SKIP LOCKED` prevents duplicate processing across instances
- **API gateway** — Nginx reverse proxy provides a single entry point
- **Problem Details (RFC 9457)** — standardized error responses across all endpoints
- **File validation** — structural validation (headers, column count) at ingestion, rejects invalid files before they
  enter the pipeline
- **JPA + JdbcTemplate hybrid** — JPA for CRUD-heavy services, raw JDBC for the batch processor where explicit SQL
  control matters
- **JWT authentication** — Keycloak-issued tokens with role-based access control per endpoint
- **Docker Compose orchestration** — sequential startup with health checks, memory limits, and JVM tuning

## Prerequisites

- Java 25 (or compatible JDK)
- Docker and Docker Compose

## Tech Stack

| Layer            | Technology                     |
|------------------|--------------------------------|
| Language         | Kotlin 2.2                     |
| Framework        | Spring Boot 4.0                |
| Web              | Spring Web MVC                 |
| Messaging        | Apache Kafka (Confluent)       |
| Database         | PostgreSQL 18                  |
| ORM              | JPA/Hibernate (ingest, report) |
| Data Access      | JdbcTemplate (processing)      |
| Auth             | Keycloak (OpenID Connect)      |
| API Gateway      | Nginx                          |
| Containerization | Docker Compose                 |
| Monitoring       | Spring Actuator                |
| Testing          | JUnit 5, Spring Test           |

## Architecture Decisions

- **Kafka over direct HTTP** — services are decoupled. If the processing service is down, events queue up in Kafka and
  get processed when it comes back.
- **Database per service** — each service has its own PostgreSQL database. No shared schemas, no cross-service foreign
  keys.
- **JdbcTemplate for processing** — the processing service uses raw SQL instead of JPA to avoid Hibernate flush
  conflicts in batch operations and to keep database interactions explicit.
- **Scheduled batches over real-time processing** — files accumulate and are processed on a timer, demonstrating the
  timed batch processing pattern used in ETL pipelines and data ingestion systems.
- **Nginx gateway** — a single entry point hides internal service topology. In production, this would be replaced by a
  cloud load balancer or Kubernetes ingress.

## Architecture Diagram

```
                    ┌──────────────┐
                    │   Client     │
                    └──────┬───────┘
                           │
                    POST /api/v1/uploads
                    GET  /api/v1/reports
                           │
                    ┌──────▼───────┐
                    │  API Gateway │  (Nginx)
                    │   port 80    │
                    └──────┬───────┘
                           │
            ┌──────────────┼──────────────┐
            │                             │
    ┌───────▼──────┐              ┌───────▼──────┐
    │    Ingest    │              │    Report    │
    │   Service    │              │   Service    │
    │  (port 8081) │              │  (port 8083) │
    └───────┬──────┘              └───────▲──────┘
            │                             │
     file.uploaded                 file.processed
      (Kafka topic)                 (Kafka topic)
            │                             │
            │       ┌─────────────┐       │
            └──────►│  Processing │───────┘
                    │   Service   │
                    │  (port 8082)│
                    └─────────────┘
```

### Flow

1. **Ingest Service** receives files via REST (CSV, JSON, XML), validates structure, stores the file in PostgreSQL, and
   publishes a `file.uploaded` event to Kafka.

2. **Processing Service** consumes events from Kafka, queues jobs in its database, and processes them in scheduled
   batches every 30 seconds. It downloads the file from the ingest service, parses it, computes aggregations (revenue
   totals, category breakdowns, percentage distributions, date ranges), and publishes a `file.processed` event.

3. **Report Service** consumes processed events and builds queryable summaries. Clients retrieve reports through the API
   gateway.

## Access Points

| Service          | URL                             |
|------------------|---------------------------------|
| Upload endpoint  | http://localhost/api/v1/uploads |
| Reports endpoint | http://localhost/api/v1/reports |


## Authentication

The pipeline uses Keycloak for JWT-based authentication with role-based access control.

### Realm: `microservices-realm`

| Client               | Roles                      | Purpose                          |
|----------------------|----------------------------|----------------------------------|
| ingest-service       | `ingest:write`, `ingest:read` | Upload and retrieve files     |
| processing-service   | `ingest:read`              | Fetch files for batch processing |
| report-service       | `report:read`              | Query processed reports          |

### Getting a token

```bash
curl -X POST http://keycloak:8080/realms/microservices-realm/protocol/openid-connect/token \
  -d "client_id=ingest-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials"
```

Then include it in requests:

```bash
curl -H "Authorization: Bearer <token>" \
  -F "file=@data.csv" \
  http://localhost/api/v1/uploads
```

## Getting Started

1. Clone the repository:

```bash
git clone https://github.com/your-username/file-pipeline.git
cd file-pipeline
```

2. Copy the environment files:

```bash
cp .env.example .env
cp kafka.env.example kafka.env
```

3. Build and start all services:

```bash
chmod +x build-all.sh
./build-all.sh
```

This builds each service locally and starts the full stack with Docker Compose.

## Usage

### Upload a file

```bash
# CSV
curl -F "file=@data.csv" http://localhost/api/v1/uploads

# JSON
curl -F "file=@data.json" http://localhost/api/v1/uploads

# XML
curl -F "file=@data.xml" http://localhost/api/v1/uploads
```

### Check reports

```bash
# List all summaries
curl http://localhost/api/v1/reports

# Get a specific file summary
curl http://localhost/api/v1/reports/files/{fileId}

# Filter by status
curl "http://localhost/api/v1/reports?status=COMPLETED"
```

### Expected file format

All file formats must contain records with these fields: `date`, `product`, `region`, `revenue`, `quantity`.

**CSV:**

```csv
date,product,region,revenue,quantity
2026-01-15,Widget A,North,15000.50,120
2026-02-01,Widget B,South,8200.00,65
```

**JSON:**

```json
[
  {
    "date": "2026-01-15",
    "product": "Widget A",
    "region": "North",
    "revenue": 15000.50,
    "quantity": 120
  },
  {
    "date": "2026-02-01",
    "product": "Widget B",
    "region": "South",
    "revenue": 8200.00,
    "quantity": 65
  }
]
```

**XML:**

```xml

<records>
    <record>
        <date>2026-01-15</date>
        <product>Widget A</product>
        <region>North</region>
        <revenue>15000.50</revenue>
        <quantity>120</quantity>
    </record>
</records>
```

### Sample report response

```json
{
  "id": "e9c365cb-0e04-4889-a855-01c2c38b4b3d",
  "fileId": "46e5ad5b-3d1d-4b99-9510-c12104f3870e",
  "filename": "sales-data.csv",
  "status": "COMPLETED",
  "totalRows": 8,
  "validRows": 8,
  "invalidRows": 0,
  "summaryData": {
    "totalRevenue": 96002.0,
    "totalQuantity": 753,
    "uniqueProducts": 3,
    "uniqueRegions": 4,
    "revenueByProduct": {
      "Widget A": 46201.25,
      "Widget B": 33100.25,
      "Widget C": 16700.5
    },
    "revenueByRegion": {
      "North": 26500.75,
      "South": 27100.0,
      "East": 19901.25,
      "West": 22500.0
    },
    "productDistribution": {
      "Widget A": 37.5,
      "Widget B": 37.5,
      "Widget C": 25.0
    },
    "regionDistribution": {
      "North": 25.0,
      "South": 25.0,
      "East": 25.0,
      "West": 25.0
    },
    "dateRange": {
      "earliest": "2026-01-15",
      "latest": "2026-03-15",
      "spanDays": 59
    }
  },
  "processedAt": "2026-04-02T15:07:11.865148Z"
}
```

## Running Tests

```bash
cd ingest-service && ./gradlew test && cd ..
cd processing-service && ./gradlew test && cd ..
cd report-service && ./gradlew test && cd ..
```



