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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    private static final String CONTEXT_PATH = "/api/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        adminUserRepository.deleteAll();
    }

    @Test
    void testSuccessfulRegistration() throws Exception {
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
                .andExpect(jsonPath("$.data.user.email", is("tester@example.com")))
                .andExpect(jsonPath("$.data.token", notNullValue()));
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

        mockMvc.perform(post(CONTEXT_PATH + "/auth/login")
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void testInvalidCredentials() throws Exception {
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
}
