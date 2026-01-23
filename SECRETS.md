# Application Secrets & Credentials

## Configuration Secrets

The application uses environment variables for sensitive configuration.

| Property | Environment Variable | Default (Dev) | Description |
|----------|---------------------|---------------|-------------|
| `jwt.secret` | `JWT_SECRET` | `404E63...` (See application.yml) | Secret key for signing JWTs. |

### How to set secrets
**Production:** Set the `JWT_SECRET` environment variable in your deployment environment (e.g., Docker, Kubernetes, AWS).
**Development:** The application defaults to a hardcoded development secret if the environment variable is not set.

## Mock Credentials (for Testing)

The application uses an in-memory mock user store (`UserService.java`).

**Login Endpoint:** `POST /auth/login`

| Username | Password | Role |
|----------|----------|------|
| `user0`  | *any*    | ADMIN |
| `user1`  | *any*    | ADMIN |
| ...      | ...      | ...   |
| `user49` | *any*    | ADMIN |

**Note:** For the mock users, password validation is **skipped** in `AuthService.java` because the mock `User` object does not store password hashes. You can use any string as a password.

**Registration:**
You can register a new user via `POST /auth/register`.
Payload:
```json
{
  "username": "newuser",
  "emailAddress": "new@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```
Newly registered users are added to the in-memory list and will persist until the application restarts.
