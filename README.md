# Patient Management System

A microservices-based Patient Management System built with Spring Boot, gRPC, Kafka, PostgreSQL, and deployed locally using LocalStack + AWS CDK (Java).

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Running Locally with LocalStack](#running-locally-with-localstack)
6. [Using the API](#using-the-api)
7. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

| Service | Port | Description |
|---|---|---|
| `api-gateway` | 4004 | Single entry point; routes REST traffic to internal services |
| `auth-service` | 4005 | JWT login and token validation |
| `patient-service` | 4000 | Patient CRUD (PostgreSQL + Kafka producer + gRPC client) |
| `billing-service` | 4001 / 9001 | gRPC server for billing accounts |
| `analytics-service` | 4002 | Kafka consumer for analytics events |
| `infrastructure` | — | AWS CDK app provisioning VPC, RDS, MSK, ECS, ALB on LocalStack |

Communication flow:
- Client → REST → API Gateway → auth-service / patient-service
- patient-service → gRPC → billing-service
- patient-service → Kafka (MSK) → analytics-service

---

## Tech Stack

- Java 25 + Spring Boot 4
- Maven (multi-module)
- PostgreSQL (per-service RDS via LocalStack)
- Apache Kafka (MSK via LocalStack)
- gRPC + Protocol Buffers
- AWS CDK (Java) for IaC
- LocalStack for local AWS emulation
- Docker

---

## Project Structure

```
patient-management/
├── api-gateway/          # Spring Cloud Gateway
├── auth-service/         # Authentication / JWT
├── patient-service/      # Patient CRUD + Kafka producer + gRPC client
├── billing-service/      # gRPC server
├── analytics-service/    # Kafka consumer
├── infrastructure/       # AWS CDK stack (LocalStack target)
├── integration-test/     # End-to-end integration tests
├── api-requests/         # .http files for REST testing (IntelliJ)
└── grpc-requests/        # .http files for gRPC testing
```

---

## Prerequisites

- JDK 25 (Eclipse Temurin recommended)
- Maven 3.9+
- Docker + Docker Desktop
- AWS CLI v2

Verify:
```bash
java -version && mvn -version && docker --version && aws --version
```

---

## Running Locally with LocalStack

### Step 1 — Start LocalStack

First time:
```bash
docker run -d \
  --name localstack_main \
  -p 4566:4566 \
  -e SERVICES=cloudformation,ec2,ecs,elbv2,rds,msk,s3,iam,logs,route53 \
  localstack/localstack
```

After first time (already created):
```bash
docker start localstack_main
```

Verify it's healthy:
```bash
curl http://localhost:4566/_localstack/info
```

---

### Step 2 — Build Docker images

```bash
cd /path/to/patient-management

docker build -t auth-service       ./auth-service
docker build -t patient-service    ./patient-service
docker build -t billing-service    ./billing-service
docker build -t analytics-service  ./analytics-service
docker build -t api-gateway        ./api-gateway
```

---

### Step 3 — Synthesize the CDK stack

Run the `main` method in `infrastructure/src/main/java/com/pm/stack/LocalStack.java` from IntelliJ, or:

```bash
cd infrastructure
mvn compile exec:java -Dexec.mainClass="com.pm.stack.LocalStack"
```

This generates `cdk.out/localstack.template.json`.

---

### Step 4 — Set environment variables

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
```

> **Windows/WSL users:** `export` only lasts for the current terminal session. Either re-run these before each deploy or add them to your `~/.bashrc`.

---

### Step 5 — Deploy to LocalStack

```bash
aws --endpoint-url=http://localhost:4566 cloudformation deploy \
  --template-file cdk.out/localstack.template.json \
  --stack-name localstack
```

Or use the helper script:
```bash
cd infrastructure
chmod +x localstack-deploy.sh
./localstack-deploy.sh
```

---

### Step 6 — Verify

```bash
docker ps
```

You should see 5 service containers + `localstack_main`.

Get the ALB DNS:
```bash
aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
  --query "LoadBalancers[0].DNSName" --output text
```

---

## Using the API

All requests go through the API Gateway on port `4004`.  
Base URL: `http://lb-XXXXXXXX.elb.localhost.localstack.cloud:4004`

Pre-built `.http` files are in `api-requests/`. Open in IntelliJ and click ▶ to run.

### 1. Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "testuser@test.com",
  "password": "password123"
}
```
Save the returned `token`.

### 2. Patient endpoints
```http
GET    /api/patients              # list all
POST   /api/patients              # create
PUT    /api/patients/{id}         # update
DELETE /api/patients/{id}         # delete
```
All require `Authorization: Bearer {{token}}`.

---

## Troubleshooting

| Issue | Solution |
|---|---|
| LocalStack not responding | `docker restart localstack_main` |
| CloudFormation deploy failed | `aws --endpoint-url=http://localhost:4566 cloudformation delete-stack --stack-name localstack` then redeploy |
| Service stuck on startup (no "Started" log) | SSL issue with LocalStack RDS — verify `?sslmode=disable` is in datasource URL in `LocalStack.java` |
| 500 on patient create | billing-service unreachable — check `BILLING_SERVICE_ADDRESS` env var points to `localhost.localstack.cloud` |
| `.http` requests return 404 | ALB DNS changed after redeploy — fetch new DNS (Step 6) and update `.http` files |
| Port already in use | `docker ps` then `docker stop <id>` |
| Duplicate containers after redeploy | `docker ps --filter "name=ls-ecs"` — stop old ones manually |

### Clean up
```bash
docker stop localstack_main && docker rm localstack_main
docker ps -a --filter "name=ls-ecs" -q | xargs docker rm -f
```

---

## License

For educational purposes.