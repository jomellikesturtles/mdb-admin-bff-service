# MDB ADMIN BFF SERVICE (MDB-ADMIN-BFF-SERVICE)
MDB API is a Backend For Frontend (BFF) service designed to power the internal MDB admin web-app application. It aggregates data, manages users,  (user banning, region locking, stream locking), and handles media streaming logic across different regions.



## 🍃 Story time

The codes in this repository are written entirely by Agentic AI (NOT). But why do it, Jommel? 
Because I paid xxxxx amount of pesos to robot to do its damn job.
But for real, is to demonstrate the power of AI. A buzzword in the year 2026, the 'Cloud' in 2016, 'Blockchain' in 2015, 'Chuvachoochoo' in 2003

Years ago, a good employer of mine let us use and handle CoPilot.
We were a pioneer team tasked to develop features from JIRA to Pull Request. 
At first, I was hesitant to its output, to me it is just a fancy autocomplete, while I am aware that the technology is still in its infancy,
I saw myself frantically pressing tab to every suggestion it makes like what I did when I first discovered the intellisense.


Weeks worth of use really was too short to write proper a review about a lifetime Copilot (pun intended), but I saw myself using it as a work buddy and enemy in the future.
Like the developers before me sees autocomplete as a lazy way to write a code, and using an IDE to the generations before them, saw it as a threat to job security.
Will it ever replace us? I have no idea, just because you now have a car that doesn't mean it will replace walking, someone still has to drive the car.

It is not perfect, far from it. But it is better than my programming? Not really (I think), but it can deliver more code lines more than I ever could.
Does it really care about the end goal? No, at least a junior developer has a heart. 


## 🚀 Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.3.13
- **Build Tool:** Gradle
- **Database:**
    - PostgreSQL (Primary Data Store)
    - Redis (Caching)
- **Communication:**
    - **REST:** Spring MVC
    - **gRPC:** Media stream handling (`MediaStream.proto`)
    - **Feign:** External API integration (Netflix, OpenDota, etc.)
- **Security:**
    - JWT Authentication (Auth0 / JJWT)
    - Argon2id Password Hashing
    - PGP Encryption

## 🏗 Architecture

The project follows a **Backend For Frontend (BFF)** pattern (`com.mdb.bff`), serving as an orchestration layer for client applications.

### Key Features
- **Region Strategy:** Implements `RegionFactory` and `RegionService` to handle region-specific logic (e.g., `Asia`).
- **Media Streaming:** gRPC integration for high-performance media metadata or stream handling.
- **User Management:** Full profile management, favorites, bookmarks, and watchlists.

## 🛠 Getting Started

### Prerequisites
- JDK 21
- Docker (for PostgreSQL and Redis)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd mdb-api
   ```

2. **Start Infrastructure (PostgreSQL & Redis)**
   Ensure you have PostgreSQL and Redis running. You can use Docker:
   ```bash
   # Example for Redis
   docker run -d -p 6379:6379 redis

   # Example for PostgreSQL
   docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password postgres
   ```

3. **Configure Application**
   Check `src/main/resources/application.yml` and set your database credentials and API keys.

4. **Build and Run**

   This project uses Protocol Buffers (Protobuf). You must generate the Java sources from the `.proto` files before compiling or running the application.

    ```bash
    ./gradlew generateProto
    ./gradlew build
    ```
   ```bash
   ./gradlew bootRun`` --args='--spring.profiles.active=dev'
   ```

## 📂 Project Structure

```
src/main/java/com/mdb/bff/
├── clients/       # Feign clients for external APIs
├── config/        # Spring configuration (Security, Async, MVC)
├── controller/    # REST API Endpoints
├── dto/           # Data Transfer Objects
├── entity/        # JPA Entities (PostgreSQL)
├── proto/         # gRPC Service Definitions
├── service/       # Business Logic
└── ...
```

---

## 📝 Developer Notes & API Reference (Legacy)

*The following section contains notes from previous development cycles and may need verification.*

### Key Commands
- **Generate Keystore:**
  ```bash
  keytool -genkeypair -alias springboot -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650 -dname "CN=localhost, OU=IT, O=MyCompany, L=MyCity, ST=MyState, C=MyCountry"
  ```
- **Heroku DB Push:**
  ```bash
  heroku pg:push ${local_db_name} ${addon} --app ${heroku_app_name}
  ```

### API Flow Logic
**User Data:**
- `/user/:username`
    - **Online:** `userService -> bffService`
    - **Offline:** `userService -> ipcService` (if applicable)

**Lists:**
- `/list/:listId` -> `bffService` -> `ListsService` -> `ListsRepository` -> `DB`

### Endpoint Reference
- **Media:** `/media/${ID}`
- **Rate:** `/media/${ID}/rate`
- **Review:** `/media/${ID}/review`
- **User Favorites:** `/user/${ID}/favorites`
- **User Bookmarks:** `/user/${ID}/bookmarks`