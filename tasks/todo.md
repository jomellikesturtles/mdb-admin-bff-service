# Plan: Migrate to gRPC Gateways

## Objective
Migrate user, media item, and media source JDBC/JPA repositories to use `user-data-gateway-service` and `media-data-gateway-service` gRPC blocking stubs.

## Tasks
- [x] Create `UserService.proto` and `MediaService.proto` files in `src/main/proto/`
- [x] Configure second ManagedChannel and expose gRPC blocking stub beans in `GrpcConfig.java` and `application.yml`
- [x] Refactor `KeycloakJwtAuthenticationConverter.java` to call `UserService` stub
- [x] Refactor `UserService.java`, `MediaService.java`, `DashboardService.java`, and `MediaSourceService.java` to call gRPC stubs
- [x] Remove local DB files: `UserRepository.java`, `MediaSourceRepository.java`, `MediaItemRepository.java`, and their JPA entity classes
- [x] Refactor integration tests to mock the new gRPC blocking stubs instead of repository beans
- [x] Run tests and verify build passes via `./gradlew test`

## Results
- **Protobuf Contracts:** Created `UserService.proto` and `MediaService.proto` under `src/main/proto/` and expanded `TorrentService.proto` with `DeleteTorrent` RPC.
- **gRPC Channels & Stubs:** Updated `GrpcConfig.java` and `application.yml` to define the `user-gateway` gRPC client, channel, and exposed all required blocking stub beans.
- **Service Refactoring:** Migrated direct DB dependencies (`UserRepository`, `MediaItemRepository`, `MediaSourceRepository`) to their respective gRPC stubs inside `KeycloakJwtAuthenticationConverter`, `UserService`, `MediaService`, `DashboardService`, and `MediaSourceService`.
- **Database Cleanup:** Deleted unused JPA entity classes and database repository files from the workspace.
- **Testing:** Added `@MockBean` stubs to integration tests (`AuthIntegrationTest` and `MdbAdminBffApplicationTests`). All tests execute and compile successfully.

## Phase 2: Migrate Admin User Persistence
- [x] Configure `AdminUserServiceBlockingStub` bean in `GrpcConfig.java`
- [x] Refactor `AuthService.java` to use `AdminUserService` stub for login and registration
- [x] Refactor `KeycloakJwtAuthenticationConverter.java` to verify admin presence via `AdminUserService` stub
- [x] Exclude local database autoconfig in `MdbAdminBffApplication.java`
- [x] Remove database configurations from `application.yml`
- [x] Delete `AdminUserRepository.java` and `AdminUserEntity.java`
- [x] Update integration tests in `AuthIntegrationTest.java` to mock `AdminUserService` stub and remove direct repo state manipulations
- [x] Verify test suite passes via `./gradlew test`

## Results
- **Admin gRPC Migration:** Exposed `AdminUserServiceBlockingStub` and refactored `AuthService` and `KeycloakJwtAuthenticationConverter` to fully delegate all Admin actions to the gateway service via gRPC.
- **Database Decoupling:** Excluded database auto-configuration classes in `MdbAdminBffApplication`, completely removed local datasource configurations from `application.yml`, and deleted local database persistence files (`AdminUserRepository.java` and `AdminUserEntity.java`).
- **Test Integrity:** All BFF tests compile, execute, and pass successfully using gRPC mocks.
- **E2E Live Verification:** Verified registration and login against running instances of Keycloak (port 8484) and `user-data-gateway-service` (port 6001).

## Phase 3: Registration and Login Verification
- [x] Run end-to-end integration verification for admin registration (`/auth/register`)
- [x] Verify database record creation on central gateway via `AdminUserService/GetAdminUsers`
- [x] Verify provisioning in Keycloak
- [x] Run end-to-end integration verification for admin login (`/auth/login`)
- [x] Record credentials used in verification
  *   **Username:** `adm29304`
  *   **Email:** `live29304@example.com`
  *   **Password:** `SecureLivePassword123!`

## Phase 4: Swagger UI Whitelisting
- [x] Whitelist Swagger endpoints (`/v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/webjars/**`) in `SecurityConfig.java`
- [x] Verify build compilation is successful
- [x] Run test suite and confirm all tests pass

## Phase 5: Authenticated Endpoint Verification
- [x] Refactor `DashboardService.java` to handle unimplemented/unavailable downstream gRPC stubs gracefully
- [x] Integrate `/dashboard/stats` call into the live E2E integration test using generated Bearer tokens
- [x] Verify successful HTTP status 200 and JSON response payload retrieval

