package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.*;
import com.mdb.adminbff.entity.AdminUserEntity;
import com.mdb.adminbff.exception.ApiErrorCode;
import com.mdb.adminbff.exception.ApiException;
import com.mdb.adminbff.exception.BaseApiError;
import com.mdb.adminbff.repository.AdminUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final CryptoService cryptoService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public GenericResponse<LoginResponse> login(LoginRequest request) {
        logger.info("Attempting admin login for email: {}", request.getEmail());
        
        AdminUserEntity admin = adminUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed: Email not found: {}", request.getEmail());
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            logger.warn("Login failed: Password mismatch for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!"ACTIVE".equals(admin.getStatus())) {
            logger.warn("Login failed: User status is {}: {}", admin.getStatus(), request.getEmail());
            throw new IllegalStateException("User account is not active");
        }

        logger.info("Admin login successful for email: {}", request.getEmail());
        
        String token = generateToken(admin.getEmail(), admin.getId().toString(), expiration);
        String refreshToken = generateToken(admin.getEmail(), admin.getId().toString(), refreshExpiration);

        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn((int) (expiration / 1000))
                .build();

        return GenericResponse.success(loginResponse);
    }

    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting legacy registration for username: {}", request.getUsername());
        // ... (Existing register logic for mdb_user, kept for backward compatibility if needed)
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userService.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmailAddress())
                .status("ACTIVE")
                .roles(List.of("ROLE_USER"))
                .lastLogin(LocalDateTime.now())
                .build();
        
        userService.saveUser(newUser);
        return generateAuthResponse(newUser);
    }

    @Transactional
    public GenericResponse<Map<String, Object>> registerAdmin(RegisterRequest request) {
        logger.info("Attempting admin registration for email: {}", request.getEmailAddress());

        // Sanitization
        String sanitizedUsername = sanitizeInput(request.getUsername());
        String sanitizedEmail = sanitizeInput(request.getEmailAddress()).toLowerCase();

        // Uniqueness check
        if (adminUserRepository.findByEmail(sanitizedEmail).isPresent() || 
            adminUserRepository.findByUsername(sanitizedUsername).isPresent()) {
            logger.warn("Registration failed: Email or username already exists: {}", sanitizedEmail);
            throw new ApiException(new BaseApiError(ApiErrorCode.CONFLICT));
        }

        // Hashing and Persistence
        AdminUserEntity admin = AdminUserEntity.builder()
                .username(sanitizedUsername)
                .email(sanitizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .build();

        AdminUserEntity saved = adminUserRepository.save(admin);
        logger.info("Admin registered successfully: {}", saved.getEmail());

        // Token Generation
        String token = generateToken(saved.getEmail(), saved.getId().toString(), expiration);

        // Response Construction
        Map<String, Object> userData = Map.of(
                "id", saved.getId().toString(),
                "username", saved.getUsername(),
                "email", saved.getEmail()
        );

        return GenericResponse.success("User registered successfully", Map.of(
                "user", userData,
                "token", token
        ));
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }

    public AuthResponse login(AuthRequest request) {
        logger.info("Attempting legacy login for username: {}", request.getUsername());
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.warn("Login failed: Invalid username: {}", request.getUsername());
                    return new IllegalArgumentException("Invalid username or password");
                });

        logger.info("Login successful for username: {}", request.getUsername());
        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String username = claims.getSubject();
            
            return adminUserRepository.findByEmail(username)
                    .map(admin -> {
                        String newToken = generateToken(admin.getEmail(), admin.getId().toString(), expiration);
                        String newRefreshToken = generateToken(admin.getEmail(), admin.getId().toString(), refreshExpiration);
                        return AuthResponse.builder()
                                .token(newToken)
                                .refreshToken(newRefreshToken)
                                .build();
                    })
                    .orElseGet(() -> {
                        User user = userService.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                        return generateAuthResponse(user);
                    });

        } catch (Exception e) {
            logger.warn("Invalid refresh token provided");
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(jwt)
                        .getPayload();
                
                Date expirationDate = claims.getExpiration();
                long remainingTime = expirationDate.getTime() - System.currentTimeMillis();
                
                if (remainingTime > 0) {
                    tokenBlacklistService.blacklistToken(jwt, remainingTime);
                    logger.info("Token blacklisted for logout. Expires in {} ms", remainingTime);
                }
            } catch (Exception e) {
                logger.warn("Failed to blacklist token during logout: {}", e.getMessage());
            }
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = generateToken(user.getUsername(), user.getId().toString(), expiration);
        String refreshToken = generateToken(user.getUsername(), user.getId().toString(), refreshExpiration);
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .build();
    }

    private String generateToken(String subject, String userId, long expirationTime) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .id(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }
}
