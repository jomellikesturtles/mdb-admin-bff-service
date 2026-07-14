package com.mdb.adminbff.auth;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.LoginRequest;
import com.mdb.adminbff.dto.RegisterRequest;
import com.mdb.media_data_gateway_service.grpc.MediaServiceGrpc;
import com.mdb.media_data_gateway_service.grpc.TorrentServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.AdminUserServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.AdminUserRegisterRequest;
import com.mdb.user_data_gateway_service.grpc.AdminUserRegisterResponse;
import com.mdb.user_data_gateway_service.grpc.AdminUserLoginRequest;
import com.mdb.user_data_gateway_service.grpc.AdminUserResponse;
import com.mdb.user_data_gateway_service.grpc.UserServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    private static final String CONTEXT_PATH = "/api/v1";

    @Value("${keycloak.token-uri}")
    private String tokenUri;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AdminUserServiceGrpc.AdminUserServiceBlockingStub adminUserStub;

    @MockBean
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    @MockBean
    private MediaServiceGrpc.MediaServiceBlockingStub mediaItemStub;

    @MockBean
    private TorrentServiceGrpc.TorrentServiceBlockingStub mediaSourceStub;

    @Test
    void testSuccessfulRegistration() throws Exception {
        stubKeycloakRegistration();

        when(adminUserStub.registerAdmin(any(AdminUserRegisterRequest.class)))
                .thenReturn(AdminUserRegisterResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Admin registered successfully")
                        .setId("mock-admin-id")
                        .build());

        RegisterRequest request = RegisterRequest.builder()
                .username("adminTester")
                .emailAddress("tester@example.com")
                .password("StrongPassword123!")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/register")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("User registered successfully")))
                .andExpect(jsonPath("$.data.user.username", is("adminTester")))
                .andExpect(jsonPath("$.data.user.email", is("tester@example.com")));
    }

    @Test
    void testDuplicateEmailPrevention() throws Exception {
        stubKeycloakRegistration();

        when(adminUserStub.registerAdmin(any(AdminUserRegisterRequest.class)))
                .thenReturn(AdminUserRegisterResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Username or Email already exists")
                        .build());

        RegisterRequest request = RegisterRequest.builder()
                .username("newUsername")
                .emailAddress("tester@example.com")
                .password("StrongPassword123!")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/register")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testValidationFailure_WeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("validUser")
                .emailAddress("valid@example.com")
                .password("weak")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/register")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        when(adminUserStub.loginAdmin(any(AdminUserLoginRequest.class)))
                .thenReturn(AdminUserResponse.newBuilder()
                        .setId("mock-admin-id")
                        .setUsername("loginTester")
                        .setEmail("login@example.com")
                        .setStatus("ACTIVE")
                        .build());

        LoginRequest request = LoginRequest.builder()
                .email("login@example.com")
                .password("StrongPassword123!")
                .build();

        Map<String, Object> keycloakResponse = Map.of(
                "access_token", "mock-access-token",
                "refresh_token", "mock-refresh-token",
                "expires_in", 3600
        );

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(keycloakResponse));

        mockMvc.perform(post(CONTEXT_PATH + "/auth/login")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.token", is("mock-access-token")))
                .andExpect(jsonPath("$.data.refreshToken", is("mock-refresh-token")));
    }

    @Test
    void testInvalidCredentials() throws Exception {
        when(adminUserStub.loginAdmin(any(AdminUserLoginRequest.class)))
                .thenThrow(new io.grpc.StatusRuntimeException(io.grpc.Status.UNAUTHENTICATED.withDescription("Invalid credentials")));

        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/login")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    void testActuatorHealthAllowedUnauthenticated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(CONTEXT_PATH + "/actuator/health")
                .contextPath(CONTEXT_PATH))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(200, 503)));
    }

    @Test
    void testRegistrationThenLoginFlow() throws Exception {
        stubKeycloakRegistrationAndLogin();

        when(adminUserStub.registerAdmin(any(AdminUserRegisterRequest.class)))
                .thenReturn(AdminUserRegisterResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Admin registered successfully")
                        .setId("mock-flow-admin-id")
                        .build());

        RegisterRequest registerReq = RegisterRequest.builder()
                .username("newAdminFlow")
                .emailAddress("flow@example.com")
                .password("StrongPass123!")
                .confirmPassword("StrongPass123!")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/register")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")));

        when(adminUserStub.loginAdmin(any(AdminUserLoginRequest.class)))
                .thenReturn(AdminUserResponse.newBuilder()
                        .setId("mock-flow-admin-id")
                        .setUsername("newAdminFlow")
                        .setEmail("flow@example.com")
                        .setStatus("ACTIVE")
                        .build());

        LoginRequest loginReq = LoginRequest.builder()
                .email("flow@example.com")
                .password("StrongPass123!")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/login")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.token", is("flow-access-token")))
                .andExpect(jsonPath("$.data.refreshToken", is("flow-refresh-token")));
    }

    @Test
    void testRegistrationCompensationOnDbError() throws Exception {
        stubKeycloakRegistration();

        String serverUrl = tokenUri.substring(0, tokenUri.indexOf("/realms"));
        String realm = tokenUri.substring(tokenUri.indexOf("/realms/") + 8, tokenUri.indexOf("/protocol/"));
        String deleteUserUri = serverUrl + "/admin/realms/" + realm + "/users/mock-user-id-123";

        when(adminUserStub.registerAdmin(any(AdminUserRegisterRequest.class)))
                .thenThrow(new io.grpc.StatusRuntimeException(io.grpc.Status.INTERNAL.withDescription("DB error")));

        when(restTemplate.exchange(
                eq(deleteUserUri),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        RegisterRequest request = RegisterRequest.builder()
                .username("failedDbAdmin")
                .emailAddress("failed@example.com")
                .password("StrongPassword123!")
                .build();

        mockMvc.perform(post(CONTEXT_PATH + "/auth/register")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway());

        org.mockito.Mockito.verify(restTemplate).exchange(
                eq(deleteUserUri),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }

    private void stubKeycloakRegistration() {
        String serverUrl = tokenUri.substring(0, tokenUri.indexOf("/realms"));
        String realm = tokenUri.substring(tokenUri.indexOf("/realms/") + 8, tokenUri.indexOf("/protocol/"));
        String adminUsersUri = serverUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> tokenResponse = Map.of("access_token", "mock-admin-token");
        ResponseEntity<Map> tokenEntity = ResponseEntity.ok(tokenResponse);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(URI.create(adminUsersUri + "/mock-user-id-123"));
        ResponseEntity<Void> creationEntity = ResponseEntity.status(HttpStatus.CREATED)
                .headers(responseHeaders)
                .build();

        when(restTemplate.postForEntity(eq(tokenUri), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(tokenEntity);

        when(restTemplate.postForEntity(eq(adminUsersUri), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(creationEntity);
    }

    private void stubKeycloakRegistrationAndLogin() {
        String serverUrl = tokenUri.substring(0, tokenUri.indexOf("/realms"));
        String realm = tokenUri.substring(tokenUri.indexOf("/realms/") + 8, tokenUri.indexOf("/protocol/"));
        String adminUsersUri = serverUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(URI.create(adminUsersUri + "/mock-user-id-123"));
        ResponseEntity<Void> creationEntity = ResponseEntity.status(HttpStatus.CREATED)
                .headers(responseHeaders)
                .build();

        when(restTemplate.postForEntity(eq(adminUsersUri), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(creationEntity);

        when(restTemplate.postForEntity(eq(tokenUri), any(HttpEntity.class), eq(Map.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<MultiValueMap<String, String>> entity = invocation.getArgument(1);
                    MultiValueMap<String, String> body = entity.getBody();
                    if (body != null && "client_credentials".equals(body.getFirst("grant_type"))) {
                        return ResponseEntity.ok(Map.of("access_token", "mock-admin-token"));
                    }
                    if (body != null && "password".equals(body.getFirst("grant_type"))) {
                        return ResponseEntity.ok(Map.of(
                            "access_token", "flow-access-token",
                            "refresh_token", "flow-refresh-token",
                            "expires_in", 3600
                        ));
                    }
                    return ResponseEntity.ok(Map.of());
                });
    }
}
