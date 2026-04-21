# files-pipeline

An asynchronous file processing pipeline built with **Kotlin**, **Spring Boot 4**, **Apache Kafka**, and **PostgreSQL**.
Files are ingested, validated, queued, and processed in scheduled batches across four independent microservices,
deployed on **Kubernetes (Kind)** with infrastructure services managed by **Docker Compose**.

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
- **Spring Cloud Gateway** — API gateway with rate limiting backed by Redis
- **Problem Details (RFC 9457)** — standardized error responses across all endpoints
- **File validation** — structural validation (headers, column count) at ingestion, rejects invalid files before they
  enter the pipeline
- **JPA + JdbcTemplate hybrid** — JPA for CRUD-heavy services, raw JDBC for the batch processor where explicit SQL
  control matters
- **JWT authentication** — Keycloak-issued tokens with role-based access control per endpoint
- **Kubernetes deployment** — application services run on a Kind cluster, infrastructure services on Docker Compose
- **Schema Registry with BACKWARD compatibility** — ensures Kafka messages are validated against a registered JSON
  schema before being produced, preventing malformed or unexpected data from reaching consumers.

## Prerequisites

- Java 24+
- Docker and Docker Compose
- Kind
- kubectl (Kubernetes CLI)

## Tech Stack

| Layer           | Technology                         |
|-----------------|------------------------------------|
| Language        | Kotlin 2.2                         |
| Framework       | Spring Boot 4.0                    |
| Web             | Spring Web MVC                     |
| API Gateway     | Spring Cloud Gateway + Redis       |
| Messaging       | Apache Kafka (Confluent)           |
| Schema Registry | Confluent Schema Registry          |
| Database        | PostgreSQL 18                      |
| ORM             | JPA/Hibernate (ingest, report)     |
| Data Access     | JdbcTemplate (processing)          |
| Auth            | Keycloak (OpenID Connect)          |
| Orchestration   | Kubernetes (Kind) + Docker Compose |
| Monitoring      | Spring Actuator                    |
| Testing         | JUnit 5, Spring Test               |

## Architecture Decisions

- **Kafka over direct HTTP** — services are decoupled. If the processing service is down, events queue up in Kafka and
  get processed when it comes back.
- **Database per service** — each service has its own PostgreSQL database. No shared schemas, no cross-service foreign
  keys.
- **JdbcTemplate for processing** — the processing service uses raw SQL instead of JPA to avoid Hibernate flush
  conflicts in batch operations and to keep database interactions explicit.
- **Scheduled batches over real-time processing** — files accumulate and are processed on a timer, demonstrating the
  timed batch processing pattern used in ETL pipelines and data ingestion systems.
- **Spring Cloud Gateway over Nginx** — a Spring-based gateway provides a single entry point with rate limiting
  (backed by Redis), replacing the previous Nginx reverse proxy. Being a Spring service, it fits naturally into the
  microservices ecosystem and deploys alongside the other services on Kubernetes.
- **Keycloak for auth** — each microservice is a separate OAuth2 client with scoped roles. The ingest service requires
  `ingest:write` for uploads and `ingest:read` for downloads, the processing service gets `ingest:read` to fetch files
  for batch processing, and the report service uses `report:read`. Service-to-service calls use client credentials, not
  shared secrets. Keycloak runs as an infrastructure service on Docker Compose and is accessed directly on port 8180 for
  simplicity.
- **Zero trust JWT validation** — rather than using token relay (where the gateway validates once and downstream
  services trust it), every microservice independently validates JWTs against Keycloak using Spring Security OAuth2
  Resource Server. The gateway does not hold a trusted position — each service is responsible for its own authentication
- **Schema auto-generated from Kotlin data classes** — schemas are derived directly from Kotlin data classes and
  registered automatically on the first request, keeping schema definition and code in sync without manual maintenance.
- **Kubernetes for application services, Docker Compose for infrastructure** — application microservices run on a Kind
  cluster (1 replica each, for simplicity as a portfolio project), while stateful infrastructure (PostgreSQL, Kafka,
  Schema Registry, Keycloak, Redis) stays on Docker Compose. The Kind cluster connects to the Compose network so
  services can reach the infrastructure.

## Architecture Diagram

```
                         ┌──────────────┐         POST /realms/.../token
                         │    Client    │─────────────────────┐
                         └──────┬───────┘             ┌───────▼──────┐
                                │                     │   Keycloak   │
                                │                     │  port 8180   │
                                │                     └──────────────┘                       
                                │                                                   
                       POST /api/v1/uploads + JWT                                   
                       GET  /api/v1/reports + JWT                                
                                │                                                
                    ┌───────────▼────────────┐
                    │    Gateway Service     │
                    │  (Spring Cloud Gateway)│
                    │   port 80 — rate limit │
                    └───────────┬────────────┘
                                │
          ┌─────────────────────┼────────────────────────┐
          │                     ·                        │
  ┌───────▼──────┐              ·                 ┌──────▼──────┐
  │    Ingest    │································│    Report   │
  │   Service    │  JWT         ·            JWT  │   Service   │
  │              │              ·                 │             │
  └───────┬──────┘              ·                 └──────▲──────┘
          │                JWT validation                │
          │                     ·                        │
          │                     ·                        │
   file.uploaded                ·                 file.processed
    (Kafka topic)               ·                  (Kafka topic)
          │                     ·                        │
          │       ┌─────────────▼──────────────┐         │
          └──────►│     Processing Service     │─────────┘
                  │                            │
                  └────────────────────────────┘

                 ─── Data flow    ··· JWT validation
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
Keycloak runs directly on Docker Compose and is accessible at `http://localhost:8180`.

### Realm: `microservices-realm`

| Client             | Roles                         | Purpose                          |
|--------------------|-------------------------------|----------------------------------|
| ingest-service     | `ingest:write`, `ingest:read` | Upload and retrieve files        |
| processing-service | `ingest:read`                 | Fetch files for batch processing |
| report-service     | `report:read`                 | Query processed reports          |

### Getting a token

```bash
curl -X POST http://localhost:8180/realms/microservices-realm/protocol/openid-connect/token \
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

3. Build images, start infrastructure, and create the Kind cluster:

```bash
./run build
```

This builds each service's Docker image, starts the infrastructure containers (PostgreSQL, Kafka, Schema Registry,
Keycloak, Redis), configures Schema Registry with BACKWARD compatibility, creates a Kind cluster, connects it to the
Docker Compose network, and loads the service images into the cluster.

4. Deploy and start all services:

```bash
./run start
```

This deploys the four microservices to the Kind cluster in order (ingest → processing → report → gateway),
waiting for each to become ready before proceeding.

### Other commands

| Command          | Description                                                                              |
|------------------|------------------------------------------------------------------------------------------|
| `./run build`    | Build images, start infrastructure, create Kind cluster                                  |
| `./run start`    | Deploy services to Kubernetes and wait for readiness                                     |
| `./run stop`     | Scale down Kubernetes deployments and stop infrastructure (preserves data)               |
| `./run teardown` | Delete Kubernetes resources and cluster, stop Docker Compose (optionally remove volumes) |

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


