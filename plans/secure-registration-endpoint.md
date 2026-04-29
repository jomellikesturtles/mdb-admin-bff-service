# Plan: Implement Secure Registration Endpoint

## Objective
Implement a secure registration endpoint `POST /api/v1/auth/register` for admin users, adhering to Bcrypt hashing, strict input validation, and automatic JWT generation upon success.

## Key Files & Context
- `AdminUserEntity.java`: The persistence model for admin users.
- `AdminUserRepository.java`: For database uniqueness checks and insertion.
- `AuthService.java`: Will contain the registration business logic and token generation.
- `AuthController.java`: Will expose the registration endpoint.
- `RegisterRequest.java`: Existing DTO to be updated or replaced with strict validation.

## Proposed Solution

### 1. Robust Input Validation
*   **Username**: `@NotBlank`, `@Size(min = 3, max = 20)`, `@Pattern(regexp = "^[a-zA-Z0-9]+$")`.
*   **Email**: `@Email`, `@NotBlank`, must be lowercase and unique.
*   **Password**: `@Size(min = 8)`, must contain 1 uppercase, 1 number, and 1 special character.
*   **Sanitization**: Implement logic in the service to strip HTML/Script tags (using a utility or basic regex).

### 2. Security & Persistence
*   **Duplication Check**: Verify email and username uniqueness in `AdminUserRepository` before saving.
*   **Password Hashing**: Use the configured `BCryptPasswordEncoder` (strength 12) before saving to the database.
*   **Token Generation**: Reuse `generateToken` from `AuthService` to return a JWT immediately after a successful save.

### 3. API Response Consistency
*   **Success (201)**: Use `GenericResponse` wrapper. Body should include the created user (stripped of password) and the JWT token.
*   **Conflict (409)**: Throw a custom exception (e.g., `ApiException` with `ApiErrorCode.CONFLICT`) if the user already exists.

## Implementation Steps

### Phase 1: DTO and Error Codes
1. Update `RegisterRequest.java` with the specified validation constraints.
2. Add `CONFLICT` error code to `ApiErrorCode.java` (HTTP 409).

### Phase 2: Service Logic
1. Add `registerAdmin(RegisterRequest)` to `AuthService.java`.
2. Implement sanitization, uniqueness checks, and hashing.
3. Update registration logic to use the `admin_users` table exclusively for this endpoint.

### Phase 3: Controller Update
1. Update `AuthController.register` to call the new service logic.
2. Ensure it returns `201 Created` with the required JSON structure.

## Verification & Testing
- **Unit Tests**: 
    - Success registration test.
    - Duplicate email/username prevention test.
    - Password strength validation tests.
- **Manual Verification**: 
    - Register a user via Postman and verify the hashed password in the `admin_users` table.
    - Verify that HTML tags in username/email are stripped before storage.
