package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.AuthRequest;
import com.mdb.adminbff.dto.AuthResponse;
import com.mdb.adminbff.dto.RefreshTokenRequest;
import com.mdb.adminbff.dto.RegisterRequest;
import com.mdb.adminbff.dto.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting registration for username: {}", request.getUsername());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.warn("Registration failed: Passwords do not match for username: {}", request.getUsername());
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userService.findByUsername(request.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId(UUID.randomUUID().toString()) // Mock keycloak ID
                .username(request.getUsername())
                .email(request.getEmailAddress())
                .status("ACTIVE")
                .roles(List.of("ROLE_USER"))
                .lastLogin(LocalDateTime.now())
                .build();
        
        userService.saveUser(newUser);
        logger.info("User registered successfully: {}", newUser.getUsername());

        return generateAuthResponse(newUser);
    }

    public AuthResponse login(AuthRequest request) {
        logger.info("Attempting login for username: {}", request.getUsername());
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
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // In a real implementation, we should also check if the refresh token is in a database/whitelist
            // to allow revocation of refresh tokens.
            
            logger.info("Token refresh successful for user: {}", username);
            return generateAuthResponse(user);

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
        String accessToken = generateToken(user, expiration);
        String refreshToken = generateToken(user, refreshExpiration);
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .build();
    }

    private String generateToken(User user, long expirationTime) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = Map.of(
                "roles", user.getRoles() != null ? user.getRoles() : List.of()
        );
        
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }
}
