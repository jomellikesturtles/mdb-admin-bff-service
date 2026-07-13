package com.mdb.adminbff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.*;
import com.mdb.adminbff.exception.ApiErrorCode;
import com.mdb.adminbff.exception.ApiException;
import com.mdb.adminbff.exception.BaseApiError;
import com.mdb.user_data_gateway_service.grpc.AdminUserServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.AdminUserRegisterRequest;
import com.mdb.user_data_gateway_service.grpc.AdminUserRegisterResponse;
import com.mdb.user_data_gateway_service.grpc.AdminUserLoginRequest;
import com.mdb.user_data_gateway_service.grpc.AdminUserResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final TokenBlacklistService tokenBlacklistService;
    private final AdminUserServiceGrpc.AdminUserServiceBlockingStub adminUserStub;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${keycloak.token-uri}")
    private String tokenUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public GenericResponse<LoginResponse> login(LoginRequest request) {
        logger.info("Attempting admin login via Keycloak proxy for email: {}", request.getEmail());

        // 1. Verify via remote AdminUserService that credentials are correct and admin is active
        AdminUserResponse admin;
        try {
            admin = adminUserStub.loginAdmin(AdminUserLoginRequest.newBuilder()
                    .setUsernameOrEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                logger.warn("Login failed: invalid credentials for email: {}", request.getEmail());
                throw new IllegalArgumentException("Invalid credentials");
            }
            logger.error("Admin login gRPC call failed: {}", e.getMessage());
            throw new ApiException(new BaseApiError(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "GATEWAY_ERROR", "Failed to authenticate admin user: " + e.getMessage()));
        }

        if (!"ACTIVE".equals(admin.getStatus())) {
            logger.warn("Login failed: User status is {}: {}", admin.getStatus(), request.getEmail());
            throw new IllegalStateException("User account is not active");
        }

        // 2. Exchange credentials with Keycloak for tokens
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty() && !"client-secret-placeholder".equals(clientSecret)) {
            body.add("client_secret", clientSecret);
        }
        body.add("username", admin.getUsername());
        body.add("password", request.getPassword());
        body.add("scope", "openid");

        Map<String, Object> tokenResponse = fetchTokensFromKeycloak(body);

        String token = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");

        logger.info("Keycloak login successful for email: {}", request.getEmail());

        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn != null ? expiresIn : 3600)
                .build();

        return GenericResponse.success(loginResponse);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Attempting token refresh via Keycloak proxy");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty() && !"client-secret-placeholder".equals(clientSecret)) {
            body.add("client_secret", clientSecret);
        }
        body.add("refresh_token", request.getRefreshToken());

        Map<String, Object> tokenResponse = fetchTokensFromKeycloak(body);

        String token = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public GenericResponse<Map<String, Object>> registerAdmin(RegisterRequest request) {
        logger.info("Attempting admin registration for email: {}", request.getEmailAddress());

        // Sanitization
        String sanitizedUsername = sanitizeInput(request.getUsername());
        String sanitizedEmail = sanitizeInput(request.getEmailAddress()).toLowerCase();

        // 1. Authenticate with Keycloak as Admin Client
        String adminToken = getAdminAccessToken();

        // 2. Provision User in Keycloak
        String keycloakUserId = provisionUserInKeycloak(adminToken, sanitizedUsername, sanitizedEmail, request.getPassword());

        // 3. Persist on remote gateway via gRPC with compensation on failure
        try {
            AdminUserRegisterResponse regResponse = adminUserStub.registerAdmin(AdminUserRegisterRequest.newBuilder()
                    .setUsername(sanitizedUsername)
                    .setEmail(sanitizedEmail)
                    .setPassword(request.getPassword())
                    .build());

            if (!regResponse.getSuccess()) {
                logger.warn("Admin registration failed on gateway: {}", regResponse.getMessage());
                if (regResponse.getMessage().contains("exists")) {
                    throw new ApiException(new BaseApiError(ApiErrorCode.CONFLICT));
                }
                throw new ApiException(new BaseApiError(org.springframework.http.HttpStatus.BAD_REQUEST, "REGISTRATION_FAILED", regResponse.getMessage()));
            }

            logger.info("Admin registered successfully in gateway: {}", sanitizedEmail);

            Map<String, Object> userData = Map.of(
                    "id", regResponse.getId(),
                    "username", sanitizedUsername,
                    "email", sanitizedEmail
            );

            return GenericResponse.success("User registered successfully", Map.of(
                    "user", userData
            ));
        } catch (Exception e) {
            logger.error("Gateway database save failed for registered admin: {}. Executing Keycloak compensation delete...", sanitizedEmail, e);
            deleteUserInKeycloak(adminToken, keycloakUserId);
            throw e;
        }
    }

    private String getAdminAccessToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty() && !"client-secret-placeholder".equals(clientSecret)) {
            body.add("client_secret", clientSecret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = this.restTemplate.postForEntity(tokenUri, entity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return (String) responseBody.get("access_token");
            }
            throw new ApiException(new BaseApiError(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "KEYCLOAK_AUTH_ERROR", "No access token returned from Keycloak service account"));
        } catch (Exception e) {
            logger.error("Failed to authenticate BFF admin service account with Keycloak: {}", e.getMessage());
            if (e instanceof ApiException) {
                throw (ApiException) e;
            }
            throw new ApiException(new BaseApiError(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "KEYCLOAK_AUTH_ERROR", "Failed to authenticate admin service account: " + e.getMessage()));
        }
    }

    private String provisionUserInKeycloak(String adminToken, String username, String email, String password) {
        String serverUrl = tokenUri.substring(0, tokenUri.indexOf("/realms"));
        String realm = tokenUri.substring(tokenUri.indexOf("/realms/") + 8, tokenUri.indexOf("/protocol/"));
        String adminUsersUri = serverUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        Map<String, Object> userBody = Map.of(
                "username", username,
                "email", email,
                "firstName", username,
                "lastName", "Admin",
                "enabled", true,
                "emailVerified", true,
                "requiredActions", List.of(),
                "credentials", List.of(credential)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userBody, headers);

        try {
            ResponseEntity<Void> response = this.restTemplate.postForEntity(adminUsersUri, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                URI location = response.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    return path.substring(path.lastIndexOf('/') + 1);
                }
                logger.warn("Keycloak user created but Location header was missing");
                return null;
            }
            throw new ApiException(new BaseApiError(org.springframework.http.HttpStatus.valueOf(response.getStatusCode().value()), "KEYCLOAK_CREATE_ERROR", "Unexpected status code from Keycloak user creation: " + response.getStatusCode()));
        } catch (Exception e) {
            logger.error("Failed to provision user in Keycloak: {}", e.getMessage());
            if (e instanceof ApiException) {
                throw (ApiException) e;
            }
            throw new ApiException(new BaseApiError(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "KEYCLOAK_CREATE_ERROR", "Failed to provision user in identity provider: " + e.getMessage()));
        }
    }

    private void deleteUserInKeycloak(String adminToken, String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isEmpty()) {
            return;
        }

        String serverUrl = tokenUri.substring(0, tokenUri.indexOf("/realms"));
        String realm = tokenUri.substring(tokenUri.indexOf("/realms/") + 8, tokenUri.indexOf("/protocol/"));
        String deleteUserUri = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            this.restTemplate.exchange(deleteUserUri, HttpMethod.DELETE, entity, Void.class);
            logger.info("Successfully executed Keycloak compensation: deleted user {}", keycloakUserId);
        } catch (Exception e) {
            logger.error("Failed to execute Keycloak compensation for user {}: {}", keycloakUserId, e.getMessage());
        }
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            try {
                String[] parts = jwt.split("\\.");
                if (parts.length >= 2) {
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    @SuppressWarnings("unchecked")
                    Map<?, ?> claims = objectMapper.readValue(payloadJson, Map.class);
                    Number exp = (Number) claims.get("exp");
                    if (exp != null) {
                        long expirationTimeMs = exp.longValue() * 1000;
                        long remainingTime = expirationTimeMs - System.currentTimeMillis();
                        if (remainingTime > 0) {
                            tokenBlacklistService.blacklistToken(jwt, remainingTime);
                            logger.info("Token blacklisted for logout. Expires in {} ms", remainingTime);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to blacklist token during logout: {}", e.getMessage());
            }
        }
    }

    private Map<String, Object> fetchTokensFromKeycloak(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = this.restTemplate.postForEntity(tokenUri, entity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            return responseBody != null ? responseBody : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Failed to authenticate with Keycloak: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid credentials", e);
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
