# Arc42 Architecture Documentation: showcase.user-service

## Table of Contents

1. [Introduction and Goals](#1-introduction-and-goals)
2. [Constraints](#2-constraints)
3. [Context and Scope](#3-context-and-scope)
4. [Solution Strategy](#4-solution-strategy)
5. [Building Block View](#5-building-block-view)
6. [Runtime View](#6-runtime-view)
7. [Deployment View](#7-deployment-view)
8. [Cross-cutting Concepts](#8-cross-cutting-concepts)
9. [Architecture Decisions](#9-architecture-decisions)
10. [Quality Requirements](#10-quality-requirements)
11. [Risks and Technical Debt](#11-risks-and-technical-debt)
12. [Glossary](#12-glossary)

## 1. Introduction and Goals

### 1.1 Purpose

`showcase.user-service` is a sample project that implements a simple user management microservice. The business functionality is intentionally minimal and is not the main focus. Instead, the project serves as a showcase to demonstrate how various technologies and practices are used in modern Java/Spring Boot development:

- Authentication and authorization using JSON Web Tokens (JWT)
- Database schema management with Flyway
- Multi-stage, optimized Docker image build (multi-stage build with Spring Boot layers)
- CI/CD pipeline with GitHub Actions, including automatic image build and push to the GitHub Container Registry (GHCR)
- Deployment to a local Kubernetes cluster (kind)

The application exposes two REST endpoints:

- `/login` – authenticates with username/password and returns a JWT on success
- `/user` – retrieves user data by ID, protected by a valid JWT

### 1.2 Quality Goals

| Priority | Quality Goal                             | Description                                                                                                                              |
|----------|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| 1        | Comprehensibility                        | The code should be easy to understand and follow for third parties (e.g. hiring managers, other developers).                             |
| 2        | Showcase value / demonstration character | The project should demonstrate how certain technologies and concepts are used, without being distracted by business complexity.          |
| 3        | Setup / runnability                      | Interested parties should be able to download, build, and run the project locally (including in Kubernetes) without much effort.         |
| 4        | Maintainability                          | Clean, well-structured code following common patterns (layered architecture, security configuration, tests) as evidence of code quality. |

### 1.3 Stakeholders

| Role                                                                  | Expectations                                                                                                                        |
|-----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Author (developer)                                                    | The project should serve as a reference for their own skills in job application processes.                                          |
| Interested third parties (e.g. potential employers, other developers) | Should be able to view the source code, download the project, build it, run it locally, and experiment with the provided endpoints. |

## 2. Constraints

### 2.1 Technical Constraints

| Constraint                       | Explanation                                            |
|----------------------------------|--------------------------------------------------------|
| Java 22                          | Programming language and runtime of the application    |
| Spring Boot 3.5                  | Application framework                                  |
| Maven (wrapper)                  | Build tool, bundled with the project via `mvnw`        |
| H2 database (in-memory)          | Embedded database, no external infrastructure required |
| Docker                           | Containerization of the application                    |
| GitHub Actions                   | CI/CD platform                                         |
| GitHub Container Registry (GHCR) | Image registry for publishing the Docker image         |
| kind (Kubernetes in Docker)      | Local Kubernetes cluster for demonstration purposes    |

### 2.2 Organizational Constraints

- This is a non-production showcase project maintained by a single developer.
- There is no dedicated operations team; building, testing, and deployment are done locally or via GitHub Actions.
- According to the project README, the pipeline does not automatically deploy to a production environment – the Kubernetes deployment is explicitly intended for local, manual execution.

### 2.3 Conventions

- Layered architecture following common Spring Boot conventions (controller, service, repository, entity)
- Entities are used directly; separate DTOs are omitted for the sake of simplicity
- Configuration values (e.g. the JWT secret) currently live in `application.yaml` and are not suitable for production use

## 3. Context and Scope

### 3.1 Business Context

The application provides two business-facing interfaces to a client (e.g. browser, `curl`, Postman):

- **Login**: A client sends a username and password and receives a JWT in return on success.
- **User lookup**: A client uses a valid JWT to retrieve user data by ID.

```
[Client] --(POST /login: credentials)--> [showcase.user-service] --(JWT)--> [Client]
[Client] --(GET /user?id=...: bearer token)--> [showcase.user-service] --(user data)--> [Client]
```

### 3.2 Technical Context

| Communication partner         | Interface                                     | Description                                             |
|-------------------------------|-----------------------------------------------|---------------------------------------------------------|
| Client (browser, HTTP client) | HTTP/REST (port 8080, or 30080 in Kubernetes) | Access to `/login`, `/user`, `/actuator/**`             |
| Embedded H2 database          | JDBC (in-memory, internal)                    | Persistence of user data, no external access            |
| GitHub Container Registry     | Docker registry protocol (HTTPS)              | Distribution of the Docker image via the CI/CD pipeline |
| GitHub Actions runner         | HTTPS (GitHub-internal)                       | Running tests, building, and pushing the Docker image   |

## 4. Solution Strategy

The key architectural decisions are derived directly from the quality goals:

| Quality goal      | Solution approach                                                                                                                                                  |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Comprehensibility | Classic, widely used Spring Boot layered architecture (controller / service / repository / entity), immediately understandable to Java developers.                 |
| Showcase value    | Small business scope (two endpoints), with the focus instead on technical aspects such as security, database migration, containerization, and CI/CD.               |
| Setup             | A ready-made Docker image published to GHCR, pre-configured Kubernetes manifests, a kind cluster configuration, and a detailed step-by-step guide in the README.   |
| Maintainability   | Use of established libraries (Spring Security, Flyway, JJWT, Lombok), automated tests (JUnit, AssertJ, Spring Security Test), automated builds via GitHub Actions. |

Key technology decisions:

- **Spring Boot** as the application framework, since it is the standard in the Java ecosystem and already covers most of the required cross-cutting concerns (security, data access, actuator).
- **H2 in-memory database**, to keep the project runnable without external infrastructure – important for easy local execution by third parties.
- **Flyway**, to demonstrate versioned database schema management, even though this is not strictly necessary from a business perspective with an in-memory database.
- **JWT-based, stateless authentication**, as a common pattern for REST APIs.
- **Multi-stage Docker build with Spring Boot layers**, to demonstrate best practices for lean, cache-friendly images.
- **GitHub Actions + GHCR**, to showcase a complete CI pipeline (test, build, push, cleanup) without additional infrastructure cost.

## 5. Building Block View

### 5.1 Overall System (Level 1)

```
+---------------------------------------------------------------+
|                  showcase.user-service                        |
|                                                               |
|  +-----------------+    +----------------+   +--------------+ |
|  |   auth package  |    |   user package |   |  Application | |
|  | (authentication |--->| (user          |   |  (Spring     | |
|  |  & JWT)         |    |  management)   |   |   Boot main) | |
|  +-----------------+    +----------------+   +--------------+ |
|           |                     |                             |
|           v                     v                             |
|  +------------------------------------------------------+     |
|  |              Spring Data JPA / H2 database           |     |
|  +------------------------------------------------------+     |
+---------------------------------------------------------------+
```

### 5.2 Package `de.dema.auth`

Responsible for authentication, JWT generation/validation, and security configuration.

| Building block                 | Responsibility                                                                                                                                                                                                                                                                                    |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SecurityConfig`               | Configures the Spring Security filter chain: stateless sessions, allows unauthenticated access to `/login` and `/actuator/**`, requires authentication for all other endpoints, and registers the `JwtAuthFilter`. Also provides `AuthenticationManager` and `PasswordEncoder` (BCrypt) as beans. |
| `AuthController`               | Exposes the `POST /login` endpoint. Accepts a `LoginRequest` (username/password), delegates to the `AuthenticationManager`, and returns a `JwtResponse` containing a valid token on success.                                                                                                      |
| `JwtService`                   | Generates and validates JWTs (HMAC-signed). Reads the secret and expiration time from configuration. Provides methods to generate a token (`generateToken`), extract the username (`extractUsername`), and validate a token (`isValid`).                                                          |
| `JwtAuthFilter`                | Servlet filter (`OncePerRequestFilter`) that inspects the `Authorization` header on every request. If a valid bearer token is present, the corresponding user is loaded and marked as authenticated in the `SecurityContext`.                                                                     |
| `LoginRequest` / `JwtResponse` | Simple data structures (records) for the request and response bodies of the login endpoint.                                                                                                                                                                                                       |

### 5.3 Package `de.dema.user`

Responsible for managing and providing user data.

| Building block   | Responsibility                                                                                                                                                        |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `UserController` | Exposes `GET /user/count` (number of users) and `GET /user?id=...` (user by ID).                                                                                      |
| `UserService`    | Business logic for user management. Also implements Spring Security's `UserDetailsService` to load users by name for authentication and assign them the `ADMIN` role. |
| `UserRepository` | Spring Data JPA repository for accessing `UserEntity`, including a `findByName` method.                                                                               |
| `UserEntity`     | JPA entity mapping a user (`id`, `name`, `password`, `age`) to the `APP_USER` table. The password field is excluded from JSON serialization (`@JsonIgnore`).          |

### 5.4 Package `de.dema` (root)

| Building block | Responsibility                                                                        |
|----------------|---------------------------------------------------------------------------------------|
| `Application`  | Entry point of the Spring Boot application (`@SpringBootApplication`, `main` method). |

### 5.5 Database Schema and Initial Data

- `db/migration/V1__initial_setup.sql` (Flyway migration): creates the `APP_USER` table and a corresponding sequence.
- `data.sql`: populates the table at startup with three sample users (including BCrypt-hashed passwords) that can be used to test the login endpoint.

## 6. Runtime View

### 6.1 Scenario: Login

1. A client sends `POST /login` with a username and password as a JSON body.
2. `AuthController` receives the request and creates a `UsernamePasswordAuthenticationToken`.
3. The `AuthenticationManager` validates the credentials, with `UserService` (as `UserDetailsService`) loading the user by name from the database and the `PasswordEncoder` checking the password.
4. On successful authentication, `JwtService` generates a signed JWT for the user.
5. `AuthController` returns the token as a `JwtResponse`.

```
Client -> AuthController: POST /login {username, password}
AuthController -> AuthenticationManager: authenticate(...)
AuthenticationManager -> UserService: loadUserByUsername(username)
UserService -> UserRepository: findByName(username)
AuthenticationManager -> PasswordEncoder: matches(password, hash)
AuthController -> JwtService: generateToken(username)
AuthController -> Client: 200 OK {token}
```

### 6.2 Scenario: Retrieving User Data

1. A client sends `GET /user?id=<id>` with the JWT in the `Authorization` header (`Bearer <token>`).
2. `JwtAuthFilter` extracts and validates the token. If valid, the corresponding user is loaded and marked as authenticated in the `SecurityContext`.
3. The request passes through the Spring Security filter chain and reaches `UserController`.
4. `UserController` calls `UserService.getUser(id)`.
5. `UserService` loads the `UserEntity` via `UserRepository`; if no matching user exists, an `EntityNotFoundException` is thrown.
6. The `UserEntity` is returned as JSON (the password field is hidden).

```
Client -> JwtAuthFilter: GET /user?id=2 (Authorization: Bearer ...)
JwtAuthFilter -> JwtService: extractUsername(token), isValid(token)
JwtAuthFilter -> UserService: loadUserByUsername(username)
JwtAuthFilter -> SecurityContext: setAuthentication(...)
UserController -> UserService: getUser(id)
UserService -> UserRepository: findById(id)
UserController -> Client: 200 OK {UserEntity} | 404/error
```

## 7. Deployment View

### 7.1 Infrastructure (local execution / demonstration)

The project is designed for multiple local operating modes:

**Option A – Local Docker container**

```
+----------------------------+
| Docker host                |
|  +----------------------+  |
|  | Container:           |  |
|  | showcase.user-service|  |
|  | (port 8080)          |  |
|  +----------------------+  |
+----------------------------+
        ^
        | port mapping 8080:8080
        |
   [ Client ]
```

Build: `docker build -t showcase.user-service .`
Run: `docker run -p 8080:8080 showcase.user-service`

**Option B – Image from GHCR**

The image is automatically pushed to `ghcr.io/dmasc/showcase.user-service` via the CI/CD pipeline (see section 8.1), with tags `latest` and the commit SHA, and can be pulled directly from there:
`docker pull ghcr.io/dmasc/showcase.user-service:latest`

**Option C – Local Kubernetes cluster (kind)**

```
+----------------------------------------------------------------+
| Host machine                                                   |
|                                                                |
|   localhost:30080                                              |
|        |                                                       |
|        v                                                       |
|  +-----------------------------------------------------------+ |
|  | kind node (Docker container, control plane)               | |
|  |                                                           | |
|  |  +-----------------+   NodePort 30080 -> service port 80  | |
|  |  | Service         |--------------------------------------+ |
|  |  | showcase-user-  |                                      | |
|  |  | service         |                                      | |
|  |  | (NodePort)      |                                      | |
|  |  +--------+--------+                                      | |
|  |           | port 80 -> targetPort 8080                    | |
|  |           v                                               | |
|  |  +----------------+                                       | |
|  |  | Pod            |                                       | |
|  |  | showcase-user- |                                       | |
|  |  | service        |                                       | |
|  |  | container:8080 |                                       | |
|  |  +----------------+                                       | |
|  +-----------------------------------------------------------+ |
+----------------------------------------------------------------+
```

Process:

1. Create the kind cluster with port mapping (`kind create cluster --config target/k8s/kind-config.yaml`): maps host port 30080 to the same port on the kind node.
2. Load the Docker image into the kind node's containerd image store (`kind load docker-image ...`).
3. Roll out the deployment and service via `kubectl apply -f target/k8s/deployment.yaml`.
4. Access the application via `http://localhost:30080/...`.

### 7.2 Mapping Building Blocks to Infrastructure

| Building block                         | Runtime environment                                                              |
|----------------------------------------|----------------------------------------------------------------------------------|
| Spring Boot application (all packages) | Single container, Java process (`JarLauncher`) based on `eclipse-temurin:22-jre` |
| H2 database                            | Embedded within the same Java process (in-memory)                                |
| Kubernetes deployment                  | 1 pod, 1 replica, NodePort service for external access                           |

## 8. Cross-cutting Concepts

### 8.1 Build and Deployment Pipeline (GitHub Actions)

The pipeline (`.github/workflows/build.yml`) runs on pushes to the `main` and `develop` branches:

1. Set up Java 22 (Corretto distribution)
2. Check out the source code
3. Run the tests (`./mvnw test`)
4. Log in to the GitHub Container Registry using `GITHUB_TOKEN`
5. Build the Docker image with two tags: the commit SHA and `latest`
6. Push both tags to `ghcr.io/<repository>`

Since both tags are based on the same image build, they share identical layers in the registry; no duplicate storage content is created.

There is also a separate workflow (`cleanup-ghcr.yml`), runnable daily and on demand, that cleans up old, unused images in GHCR (keeps the last 10 tagged versions and deletes untagged images).

### 8.2 Containerization

The Dockerfile uses a multi-stage build:

- **Build stage**: `maven:3.9-eclipse-temurin-22`, pre-loads dependencies, builds the application, and extracts the Spring Boot layers (`layertools`).
- **Runtime stage**: a lean `eclipse-temurin:22-jre` image, runs as a dedicated, non-privileged user (`app_user`/`app_group`), and copies the layers separately (dependencies, loader, snapshot dependencies, application) for optimal Docker layer cache usage.
- The application is started directly via `org.springframework.boot.loader.launch.JarLauncher`, with JVM memory limits set via `-XX:InitialRAMPercentage` / `-XX:MaxRAMPercentage`.

### 8.3 Security / Authentication

- Stateless authentication via JWT (no session management).
- Passwords are stored as BCrypt hashes (see `data.sql`).
- `/login` and `/actuator/**` are accessible without authentication; all other endpoints require a valid bearer token.
- The JWT signing secret is currently stored in plain text in `application.yaml` – acceptable for a pure showcase project, but in a production context this would need to be handled via secret management (see section 11).

### 8.4 Database Migration

Flyway manages the schema in a versioned way via SQL migration scripts (`db/migration/V1__initial_setup.sql`). The schema is created or updated automatically at application startup. Initial test data is additionally inserted via `data.sql`.

### 8.5 Monitoring / Health Checks

Spring Boot Actuator provides health endpoints that are used as liveness and readiness probes in Kubernetes (`/actuator/health/liveness`, `/actuator/health/readiness`).

### 8.6 Configuration

The central configuration file `application.yaml` contains, among other things:

- Database connection (H2, in-memory, PostgreSQL compatibility mode)
- Activation of the H2 console under `/db`
- JWT secret and token expiration time

## 9. Architecture Decisions

| Decision                                                          | Rationale                                                                                     | Alternatives                                                    | Consequences                                                                                                          |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| H2 in-memory database instead of an external DB (e.g. PostgreSQL) | Simple, infrastructure-free local execution for interested users; lowers the barrier to entry | PostgreSQL, MySQL via Docker Compose                            | Data is lost on every restart; H2's PostgreSQL compatibility mode is used to keep SQL as realistic as possible        |
| JWT instead of session-based authentication                       | Demonstrates stateless, REST-typical authentication; a widely used pattern                    | Session cookies, OAuth2/OIDC with an external identity provider | Higher implementation effort (custom filter, token handling); secret management kept simple                           |
| Multi-stage Dockerfile with Spring Boot layers                    | Demonstrates best practice for lean, cache-friendly images                                    | Single-stage build with a fat JAR                               | Slightly more complex Dockerfile, but a better demonstration of Docker know-how                                       |
| GitHub Actions + GHCR                                             | Free, tightly integrated with GitHub, well-suited for showcase purposes                       | GitLab CI, Jenkins, Docker Hub                                  | Dependency on GitHub infrastructure; images are publicly accessible via GHCR                                          |
| No automated deployment in the pipeline                           | Showcase character; deployment is intended to be reproduced locally by interested users       | Automated deployment to a hosted environment                    | No permanently available demo instance; users must set up the project themselves                                      |
| No DTOs, entities used directly                                   | Simplicity and clarity for a small showcase project                                           | Introduction of a separate DTO layer                            | Less code, but unusual/unclean in a real project (explicitly called out in the README as a deliberate simplification) |

## 10. Quality Requirements

### 10.1 Quality Tree (excerpt)

```
Quality
├── Comprehensibility
│   ├── Clear package structure (auth / user)
│   └── Consistent use of Spring Boot conventions
├── Setup
│   ├── Runnable without external infrastructure (H2 in-memory)
│   ├── Ready-made Docker and Kubernetes artifacts
│   └── Detailed README with step-by-step instructions
├── Testability
│   ├── Unit/integration tests for controller, service, repository
│   └── Spring Security Test for protected endpoints
└── Maintainability
    ├── Versioned database migrations (Flyway)
    └── Automated CI pipeline (tests on every push)
```

### 10.2 Quality Scenarios

| Scenario                                                                                                   | Expected outcome                                                                                                                               |
|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| A person with no prior knowledge of the project clones the repository and follows the README instructions. | The application can be started locally via Docker or in a kind cluster without any additional infrastructure (e.g. an external database).      |
| A reviewer (e.g. as part of a job application) inspects the source code.                                   | The package structure, naming, and layer separation are understandable without extensive explanation.                                          |
| A client sends an expired or invalid JWT to `/user`.                                                       | The request is not authenticated; the `JwtAuthFilter` does not set an authentication context, and the subsequent security check denies access. |
| A new commit is pushed to `main` or `develop`.                                                             | The GitHub Actions pipeline automatically runs the tests and, on success, publishes an updated Docker image to GHCR.                           |

## 11. Risks and Technical Debt

| Risk / technical debt                                 | Description                                                                              | Possible consequence                                   | Recommendation                                                                                                                                                                |
|-------------------------------------------------------|------------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JWT secret stored in plain text in `application.yaml` | The signing secret is hardcoded in the source code and therefore publicly visible        | In a production setting, tokens could be forged        | For production scenarios, provide the secret via environment variables/secret management (e.g. Kubernetes Secrets); acceptable for showcase purposes but should be documented |
| In-memory database (H2)                               | All data is lost on every restart                                                        | No persistence across restarts                         | Not critical for the showcase goal; an optional PostgreSQL variant (e.g. via Docker Compose) could be added if needed                                                         |
| No HTTPS termination in the example deployment        | Communication happens over unencrypted HTTP (NodePort)                                   | Not critical for local demos, but not production-ready | Explicitly mark as a demo setup in the README (already implied by "local cluster")                                                                                            |
| Single replica, no resilience concept                 | The deployment runs with `replicas: 1` and has no recovery strategy in case of data loss | Not relevant for a showcase                            | No action needed, as this is a deliberate simplification                                                                                                                      |

## 12. Glossary

| Term                             | Explanation                                                                                                                              |
|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| JWT (JSON Web Token)             | A compact, signed token format used to transmit authentication information between client and server.                                    |
| GHCR (GitHub Container Registry) | A registry provided by GitHub for storing and distributing container images.                                                             |
| kind (Kubernetes in Docker)      | A tool for running local Kubernetes clusters, where each cluster node runs as a Docker container.                                        |
| NodePort                         | A Kubernetes service type that exposes a service on a fixed port of every cluster node.                                                  |
| Flyway                           | A library for versioned management of database schema changes using SQL or Java migration scripts.                                       |
| BCrypt                           | A cryptographic hashing algorithm specifically designed for securely storing passwords.                                                  |
| Spring Boot layers               | The splitting of a Spring Boot artifact into multiple layers (dependencies, loader, application, etc.) to optimize Docker layer caching. |
| Actuator                         | A Spring Boot module that provides endpoints for monitoring and managing an application (e.g. health checks).                            |