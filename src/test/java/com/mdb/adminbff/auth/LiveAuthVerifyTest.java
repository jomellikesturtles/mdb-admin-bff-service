package com.mdb.adminbff.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.LoginRequest;
import com.mdb.adminbff.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8484/realms/mdb-realm",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8484/realms/mdb-realm/protocol/openid-connect/certs",
    "keycloak.token-uri=http://localhost:8484/realms/mdb-realm/protocol/openid-connect/token"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev") // Use dev profile to connect to live localhost services
public class LiveAuthVerifyTest {

    private static final String CONTEXT_PATH = "/api/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLiveRegistrationAndLoginFlow() throws Exception {
        long timestamp = System.currentTimeMillis();
        String suffix = String.valueOf(timestamp).substring(8);
        String username = "adm" + suffix;
        String email = "live" + suffix + "@example.com";
        String password = "SecureLivePassword123!";

        System.out.println("=== Running Live E2E Registration and Login Verification ===");
        System.out.println("Username: " + username);
        System.out.println("Email: " + email);

        // 1. Call Register Endpoint
        RegisterRequest registerReq = RegisterRequest.builder()
                .username(username)
                .emailAddress(email)
                .password(password)
                .confirmPassword(password)
                .build();

        System.out.println("Invoking /auth/register...");
        mockMvc.perform(post(CONTEXT_PATH + "/auth/register")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.user.username", is(username)))
                .andExpect(jsonPath("$.data.user.email", is(email)));

        System.out.println("Registration successful!");

        // 2. Call Login Endpoint
        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        System.out.println("Invoking /auth/login...");
        String responseJson = mockMvc.perform(post(CONTEXT_PATH + "/auth/login")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        System.out.println("Login successful!");

        // Parse token
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(responseJson);
        String token = rootNode.path("data").path("token").asText();

        // 3. Call Dashboard Stats Endpoint with Bearer token
        System.out.println("Invoking /dashboard/stats with Bearer token...");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(CONTEXT_PATH + "/dashboard/stats")
                .contextPath(CONTEXT_PATH)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemHealth", notNullValue()))
                .andExpect(jsonPath("$.totalUsers", notNullValue()))
                .andExpect(jsonPath("$.totalMovies", notNullValue()));

        System.out.println("/dashboard/stats successfully retrieved! E2E flow verified.");
    }
}
