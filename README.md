# CodeSentinel AI

Enterprise Code Reverse Engineering & Security Intelligence Platform

## Architecture

- **Backend**: Java 21 + Spring Boot 3.3.5 (Microservices)
- **Frontend**: Angular 21 + Tailwind CSS v4
- **AI**: Ollama (qwen2.5:7b + nomic-embed-text)
- **Databases**: PostgreSQL, Neo4j, Redis
- **Messaging**: Apache Kafka
- **Containerization**: Docker + Kubernetes

## Services

| Service | Port | Language | Purpose |
|---|---|---|---|
| Config Server | 8888 | Java | Centralized configuration |
| Discovery Server | 8761 | Java | Service registry (Eureka) |
| API Gateway | 8080 | Java | Routing + JWT validation |
| Auth Service | 8081 | Java | Authentication + RBAC |
| Ingestion Service | 8082 | Java | File upload + GitHub clone |
| Parser Service | 8083 | Java | Code parsing (JavaParser) |
| AI Analysis Service | 8090 | Python | Ollama + RAG |
| Graph Intelligence | 8091 | Python | Neo4j graph analysis |
| Notification Service | 8092 | Python | WebSocket notifications |

## Quick Start

```bash
# Start infrastructure (databases, Kafka, Redis)
docker compose -f docker-compose.infra.yml up -d

# Start all services
docker compose up -d --build

# Check health
docker compose ps