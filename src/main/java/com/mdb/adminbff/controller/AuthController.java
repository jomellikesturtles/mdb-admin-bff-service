package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.*;
import com.mdb.adminbff.service.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and Registration")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login")
    @PostMapping("/login")
//    @RateLimiter(name = "login")
    public ResponseEntity<GenericResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Register")
    @PostMapping("/register")
    public ResponseEntity<GenericResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdmin(request));
    }

    @Operation(summary = "Refresh Token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
