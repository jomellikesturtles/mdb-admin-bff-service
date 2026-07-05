package com.mdb.adminbff.config;

import com.mdb.adminbff.entity.AdminUserEntity;
import com.mdb.adminbff.entity.UserEntity;
import com.mdb.adminbff.repository.AdminUserRepository;
import com.mdb.adminbff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Extract Keycloak Realm Roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
            Collection<?> roles = (Collection<?>) realmAccess.get("roles");
            roles.stream()
                 .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
                 .forEach(authorities::add);
        }

        // 2. Extract Keycloak Client Roles (resource_access)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<?, ?> clientConfig = (Map<?, ?>) entry.getValue();
                    if (clientConfig.get("roles") instanceof Collection) {
                        Collection<?> clientRoles = (Collection<?>) clientConfig.get("roles");
                        clientRoles.stream()
                             .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
                             .forEach(authorities::add);
                    }
                }
            }
        }

        // 3. User Identity and Local Status Verification
        String email = jwt.getClaimAsString("email");
        String keycloakId = jwt.getSubject(); // sub claim

        // Check if Admin
        if (email != null) {
            Optional<AdminUserEntity> adminOptional = adminUserRepository.findByEmail(email);
            if (adminOptional.isPresent()) {
                AdminUserEntity admin = adminOptional.get();
                if (!"ACTIVE".equals(admin.getStatus())) {
                    throw new OAuth2AuthenticationException(new OAuth2Error(
                            OAuth2ErrorCodes.ACCESS_DENIED, "Admin account is not active: " + admin.getStatus(), null));
                }
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                return new JwtAuthenticationToken(jwt, authorities, email);
            }
        }

        // Check if Regular Client User
        if (keycloakId != null) {
            Optional<UserEntity> userOptional = userRepository.findByKeycloakId(keycloakId);
            if (userOptional.isPresent()) {
                UserEntity user = userOptional.get();
                if (!"ACTIVE".equals(user.getStatus())) {
                    throw new OAuth2AuthenticationException(new OAuth2Error(
                            OAuth2ErrorCodes.ACCESS_DENIED, "User account is not active: " + user.getStatus(), null));
                }
                if (user.getRoles() != null) {
                    user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase()))
                            .forEach(authorities::add);
                }
                String username = user.getUsername() != null ? user.getUsername() : email;
                return new JwtAuthenticationToken(jwt, authorities, username);
            }
        }

        // Neither admin nor registered regular user
        throw new OAuth2AuthenticationException(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN, "User is not registered in the local database", null));
    }
}
