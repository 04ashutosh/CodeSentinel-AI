# CodeSentinel AI Platform

CodeSentinel AI is an advanced, microservices-based AI code analysis platform. It allows users to ingest Java projects (via ZIP upload or Git URL), parses the source code into an Abstract Syntax Tree (AST), stores the structural relationships in a Neo4j graph database, and provides an interactive AI Chat interface powered by LangChain and Ollama to answer complex architectural and codebase queries.

---

## 🏗 Architecture Overview

The platform is built using a modern Spring Boot microservices architecture, communicating asynchronously via Apache Kafka and synchronously via a Spring Cloud API Gateway.

### Core Services

1. **Config Server** (`config-service` - Port 8888)
   - Centralized configuration management for all Spring Boot services.
2. **Discovery Server** (`discovery-service` - Port 8761)
   - Netflix Eureka server for service registration and discovery.
3. **API Gateway** (`gateway-service` - Port 8080)
   - Spring Cloud Gateway routing requests to the appropriate microservices and handling CORS.
4. **Ingestion Service** (`ingestion-service` - Port 8081)
   - Handles uploading `.zip` files and cloning Git repositories using `JGit`.
   - Stores raw files in a local volume and publishes `project-ingestion` events to Kafka.
5. **Parser Service** (`parser-service` - Port 8082)
   - Consumes Kafka events, parses Java source code using `JavaParser`, extracts classes, interfaces, methods, fields, and relationships.
   - Publishes the structured AST graph data back to Kafka (`graph-update`).
6. **Graph Intelligence Service** (`graph-intelligence-service` - Python/FastAPI - Port 8091)
   - Consumes the AST data and persists it as Nodes and Edges in Neo4j.
   - Exposes REST endpoints to query the dependency graph.
7. **AI Analysis Service** (`ai-analysis-service` - Python/FastAPI - Port 8090)
   - LangChain-powered RAG agent connecting to a local `Ollama` instance (Llama 3).
   - Dynamically fetches graph data directly from the Graph Intelligence Service, injects it into the LLM context, and answers user queries about the codebase.
8. **Frontend UI** (`codesentinel-ui` - Angular - Port 4200)
   - A modern Angular 21 dashboard for uploading projects, pasting Git URLs, and chatting with the AI.

### Infrastructure Components
- **PostgreSQL**: Relational database for storing user data, project metadata, and ingestion statuses.
- **Neo4j**: Graph database for storing code structure (Classes, Packages, Interfaces, Methods) and relationships (`EXTENDS`, `IMPLEMENTS`, `CONTAINS`, `CALLS`).
- **Apache Kafka & Zookeeper**: Message broker for event-driven asynchronous processing.
- **Redis**: Caching layer (ready for future scaling).
- **Ollama**: Local LLM engine running `llama3`.

---

## 🚀 Getting Started

### Prerequisites
- **Java 21**
- **Node.js 24+** & **Angular CLI 21+**
- **Docker & Docker Compose**
- **Ollama** installed on your host machine with the `llama3` model pulled (`ollama run llama3`).

### 1. Start the Infrastructure
First, spin up the backing services (Kafka, Postgres, Neo4j, Redis):
```bash
docker compose up -d codesentinel-postgres codesentinel-neo4j codesentinel-zookeeper codesentinel-kafka codesentinel-redis
```

### 2. Build and Start the Microservices
The `docker-compose.yml` is configured to build the services from source. Start the core platform:
```bash
docker compose up -d --build
```
*(Note: It may take a minute for all services to register with the Eureka Discovery Server at `http://localhost:8761`)*

### 3. Start the Angular UI
Open a new terminal and run the frontend:
```bash
cd codesentinel-ui
npm install
ng serve
```
The application will be available at [http://localhost:4200](http://localhost:4200).

---

## 🔄 End-to-End Workflow

1. **Ingestion**: 
   - User opens the UI and pastes a public Git URL (e.g., `https://github.com/spring-projects/spring-petclinic.git`).
   - The UI calls the Gateway -> `ingestion-service`. 
   - `ingestion-service` clones the repo to disk and publishes a Kafka event.
2. **Parsing**:
   - `parser-service` hears the event, reads the Java files, builds an AST, and publishes a graph payload to Kafka.
3. **Graph Storage**:
   - `graph-intelligence-service` consumes the AST payload and creates Neo4j nodes (Packages, Classes, Interfaces) and edges.
4. **AI Analysis**:
   - User types a question in the chat: *"What classes are in the common.dto package?"*
   - UI calls `ai-analysis-service`. 
   - The Python service queries the Graph service for the project's dependency graph, formats it, injects it into the LangChain prompt, and queries Ollama.
   - The LLM streams back an accurate, context-aware answer based on the actual codebase structure.

---

## 🛠 Tech Stack Details
- **Backend**: Spring Boot 3.x, Spring Cloud, Java 21, Spring Data JPA, Spring Kafka.
- **Python Services**: FastAPI, LangChain, Pydantic, Neo4j Python Driver.
- **Frontend**: Angular 21, SCSS, RxJS.
- **Data**: PostgreSQL, Neo4j, Apache Kafka, Redis.
- **AI**: Local LLM via Ollama (`llama3`).
- **Parsing**: Eclipse JGit, JavaParser.
