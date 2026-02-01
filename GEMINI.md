# mdb-admin-bff-service Context for AI Agents

- auto-save analysis to an .md file into /notes folder
- store every prompt into a file
## Project Overview
**mdb-admin-bff-service** is a Backend-for-Frontend (BFF) service built with Spring Boot. It aggregates media metadata, handles user preferences, and manages streaming sources.

## Core Architecture
- **Framework:** Spring Boot 3.x (Java 17+)
- **Build Tool:** Gradle (Wrapper available)
- **Caching:** Redis (Custom implementation via `RedisCache.java`)

## Key Development Patterns

### 1. gRPC Clients
- Do **not** use `@GrpcService` on client classes. Use standard `@Service`.
- All clients share a single `ManagedChannel` bean defined in `GrpcConfig.java`.
- Proto files are located in `src/main/proto/`. Keep them separated by domain.

### 2. API & Swagger
- **Contract:** Defined in `src/main/resources/swagger.yml`.
- **Conventions:**
    - No trailing slashes in paths.
    - User-centric endpoints use `GET` for retrieval (even if the controller implies logic).
    - Use `isEnabled` naming convention for booleans (e.g., `isAutoScanEnabled`).

## Environment & Setup
- **Context Path:** `/mdb` (Configured in `application-dev.yml`).
- **WebSockets:**
    - Endpoint: `/gs-guide-websocket`
    - URL: `ws://localhost:8080/mdb/gs-guide-websocket`
    - Protocol: STOMP (Requires CONNECT/SUBSCRIBE frames).
- **SSL:** Enabled by default in most profiles. Development often requires disabling SSL verification or using the self-signed `keystore.p12`.

## Common Tasks
- **Running Tests:** `./gradlew test`
- **Compiling:** `./gradlew compileJava` (Note: generated proto files must be compilable).
- **Adding a new Proto:** Add `.proto` file -> Run `./gradlew generateProto` (or build) -> Implement Client.

## Best Practices & Resources
**Instruction:** Use the following links and read subfolders in these repositories (if possible) to guide development.

- **Spring Boot:**
    - [tomoyane/springboot-bestpractice](https://github.com/tomoyane/springboot-bestpractice)
    - [arsy786/springboot-best-practices](https://github.com/arsy786/springboot-best-practices)
- **Backend:**
    - [futurice/backend-best-practices](https://github.com/futurice/backend-best-practices)
- **Java:**
    - [Google Cloud Java Best Practices](https://docs.cloud.google.com/java/docs/java-best-practices)
    - [DZone Java Best Practices Quick Reference](https://dzone.com/articles/java-best-practices-quick-reference)

