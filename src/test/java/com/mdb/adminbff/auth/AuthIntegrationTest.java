package com.mdb.adminbff.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.LoginRequest;
import com.mdb.adminbff.dto.RegisterRequest;
import com.mdb.adminbff.entity.AdminUserEntity;
import com.mdb.adminbff.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    private static final String CONTEXT_PATH = "/api/v1";

    @Value("${keycloak.token-uri}")
    private String tokenUri;

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        adminUserRepository.deleteAll();
    }

    @Test
    void testSuccessfulRegistration() throws Exception {
        stubKeycloakRegistration();

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
        // Pre-create user
        adminUserRepository.save(AdminUserEntity.builder()
                .username("existingUser")
                .email("tester@example.com")
                .password("anyPassword")
                .status("ACTIVE")
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
        // Pre-create hashed user
        adminUserRepository.save(AdminUserEntity.builder()
                .username("loginTester")
                .email("login@example.com")
                .password(passwordEncoder.encode("StrongPassword123!"))
                .status("ACTIVE")
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
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("WrongPassword123!")
                .build();

        // Simulate local lookup failure (nonexistent email)
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

        // 1. Register new admin
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

        // 2. Verify saved in database
        Optional<AdminUserEntity> savedAdmin = adminUserRepository.findByEmail("flow@example.com");
        assertThat(savedAdmin).isPresent();
        assertThat(savedAdmin.get().getUsername()).isEqualTo("newAdminFlow");
        assertThat(savedAdmin.get().getStatus()).isEqualTo("ACTIVE");

        // 3. Log in as the registered admin (using the stubbed credentials flow)
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

        // Mock repository save to throw an exception
        org.mockito.Mockito.doThrow(new RuntimeException("DB error"))
                .when(adminUserRepository).save(any(AdminUserEntity.class));

        // Mock Keycloak DELETE call
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
                .andExpect(status().isInternalServerError());

        // Verify that DELETE call was triggered as compensation
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
