package com.mdb.adminbff.config;

import com.mdb.adminbff.dto.User;
import com.mdb.adminbff.entity.AdminUserEntity;
import com.mdb.adminbff.repository.AdminUserRepository;
import com.mdb.adminbff.service.TokenBlacklistService;
import com.mdb.adminbff.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenBlacklistService tokenBlacklistService;
    private final UserService userService;
    private final AdminUserRepository adminUserRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            logger.warn("Attempted access with blacklisted token");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();

            if (subject != null) {
                // First check if it's an admin (subject is email in new auth)
                Optional<AdminUserEntity> adminOptional = adminUserRepository.findByEmail(subject);

                if (adminOptional.isPresent()) {
                    AdminUserEntity admin = adminOptional.get();
                    // For now, admins get a default role, or we could add roles to AdminUserEntity later
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            admin, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Fallback to regular user check (subject is username in legacy auth)
                    Optional<User> userOptional = userService.findByUsername(subject);

                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                user, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("JWT Authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
