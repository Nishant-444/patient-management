# Patient Management System

A microservices-based **Patient Management System** built with **Spring Boot**, **gRPC**, **Kafka**, **PostgreSQL**, and deployed to AWS (or **LocalStack** for local development) using **AWS CDK**.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Getting Started](#getting-started)
6. [Running Locally with LocalStack](#running-locally-with-localstack)
7. [Using the API](#using-the-api)
8. [Running Integration Tests](#running-integration-tests)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

The system is composed of the following microservices, fronted by an **API Gateway** and orchestrated on **AWS ECS Fargate** (simulated locally via LocalStack):

| Service              | Port | Description                                                         |
|----------------------|------|---------------------------------------------------------------------|
| `api-gateway`        | 4004 | Single entry point; routes traffic to internal services             |
| `auth-service`       | 4005 | Handles user login / JWT token issuing & validation                 |
| `patient-service`    | 4000 | CRUD operations for patient records (PostgreSQL)                    |
| `billing-service`    | 4001 | gRPC service for billing accounts; consumes patient events          |
| `analytics-service`  | 4002 | Kafka consumer for analytics events                                 |
| `infrastructure`     | —    | AWS CDK app that provisions VPC, RDS, MSK, ECS, ALB                 |

Communication:
- **REST/HTTP** between clients and the API Gateway.
- **gRPC** between `patient-service` and `billing-service`.
- **Kafka (AWS MSK)** for asynchronous event publishing/consumption.

---

## Tech Stack

- **Java 25** + **Spring Boot 3**
- **Maven** (multi-module)
- **PostgreSQL** (per-service RDS instances)
- **Apache Kafka** (AWS MSK)
- **gRPC + Protocol Buffers**
- **AWS CDK** (Java) for IaC
- **LocalStack** for local AWS emulation
- **Docker** for service containerization

---

## Project Structure

```
patient-management/
├── api-gateway/          # Spring Cloud Gateway
├── auth-service/         # Authentication / JWT
├── patient-service/      # Patient CRUD + Kafka producer + gRPC client
├── billing-service/      # gRPC server + Kafka consumer
├── analytics-service/    # Kafka consumer (analytics)
├── infrastructure/       # AWS CDK stack (LocalStack target)
├── integration-test/     # End-to-end integration tests
├── api-requests/         # *.http files for manual REST testing
├── grpc-requests/        # *.http files for gRPC testing
└── start-setup.txt       # Quick local-deploy notes
```

---

## Prerequisites

Install the following tools:

- **JDK 25** (Eclipse Temurin recommended)
- **Maven 3.9+**
- **Docker** & **Docker Desktop**
- **AWS CLI v2**
- **AWS CDK CLI** — `npm install -g aws-cdk`
- **LocalStack** (Docker image) — `docker pull localstack/localstack`
- **IntelliJ IDEA** (recommended; uses the included `.http` files)

Verify installations:

```bash
java -version
mvn -version
docker --version
aws --version
cdk --version
```

---

## Getting Started

### 1. Clone the repository

```bash
git clone <repo-url>
cd patient-management
```

### 2. Build all services

From the project root:

```bash
mvn clean install -DskipTests
```

This compiles every Maven module (`patient-service`, `auth-service`, `billing-service`, `analytics-service`, `api-gateway`, `infrastructure`).

### 3. Build Docker images for each service

```bash
docker build -t patient-service:latest    ./patient-service
docker build -t auth-service:latest       ./auth-service
docker build -t billing-service:latest    ./billing-service
docker build -t analytics-service:latest  ./analytics-service
docker build -t api-gateway:latest        ./api-gateway
```

> The `infrastructure` CDK stack references these image names, so make sure each one builds successfully before deployment.

---

## Running Locally with LocalStack

### Step 1 — Start LocalStack

```bash
docker run -d \
  --name localstack_main \
  -p 4566:4566 \
  -e SERVICES=cloudformation,ec2,ecs,elbv2,rds,msk,s3,iam,logs,route53 \
  localstack/localstack
```

If LocalStack is already created, simply start it:

```bash
docker start localstack_main
```

Wait until it becomes healthy:

```bash
curl http://localhost:4566/_localstack/info
```

### Step 2 — Synthesize the CDK stack

```bash
cd infrastructure
cdk synth --no-staging
```

This produces `cdk.out/localstack.template.json`.

### Step 3 — Deploy the stack to LocalStack

Configure dummy AWS credentials for LocalStack:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
```

Deploy via CloudFormation:

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

### Step 4 — Verify all services are running

```bash
docker ps
```

You should see **5 service containers** plus `localstack_main`.

Get the load balancer DNS:

```bash
aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
  --query "LoadBalancers[0].DNSName" --output text
```

---

## Using the API

The API Gateway is exposed on port **4004** via the LocalStack ALB DNS (e.g. `lb-XXXXXXXX.elb.localhost.localstack.cloud:4004`).

### 1. Login (get JWT token)

`api-requests/auth-service/login.http`:

```http
POST http://lb-XXXXXXXX.elb.localhost.localstack.cloud:4004/auth/login
Content-Type: application/json

{
  "email": "testuser@test.com",
  "password": "password123"
}
```

Copy the returned `token`.

### 2. Call patient endpoints

Pre-built requests are available under `api-requests/patient-service/`:

- `get-patients.http` — list all patients
- `create-patient.http` — create a patient
- `update-patient.http` — update a patient
- `delete-patient.http` — delete a patient

Example:

```http
GET http://lb-XXXXXXXX.elb.localhost.localstack.cloud:4004/api/patients
Authorization: Bearer <token>
```

> **Tip (IntelliJ):** Open any `.http` file and click the green ▶ icon to send the request. Replace the LB DNS placeholder with the value from Step 4.

### 3. gRPC requests

Sample gRPC requests for the billing service live in `grpc-requests/billing-service/`.

---

## Running Integration Tests

End-to-end tests sit in the `integration-test` module and assume the full stack is already running on LocalStack.

```bash
cd integration-test
mvn test
```

Or run a single service's tests:

```bash
mvn -pl patient-service test
```

---

## Troubleshooting

| Issue                                       | Solution                                                                                |
|---------------------------------------------|-----------------------------------------------------------------------------------------|
| LocalStack not responding on port 4566      | `docker restart localstack_main`; wait for `/health` endpoint to be ready               |
| CloudFormation stack stuck/failed           | Delete and redeploy: `aws --endpoint-url=http://localhost:4566 cloudformation delete-stack --stack-name patient-management` |
| Service container exits immediately         | Check logs: `docker logs <container-id>`; usually a DB connection or env var issue      |
| `.http` requests return 404                 | The LB DNS changed after redeploy — fetch the new DNS (Step 4) and update the `.http` files |
| Port already in use                         | Stop conflicting containers: `docker ps` then `docker stop <id>`                        |
| Kafka/MSK errors locally                    | LocalStack's MSK emulation is limited — ensure MSK service is enabled in LocalStack env |

### Cleaning up

```bash
docker stop localstack_main
docker rm localstack_main
docker ps -a --filter "ancestor=patient-service:latest" -q | xargs docker rm -f
```

---

## License

This project is for educational purposes.
