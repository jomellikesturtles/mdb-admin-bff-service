package com.mdb.adminbff.config;

import com.mdb.user_data_gateway_service.grpc.AdminUserServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.GetAdminUsersRequest;
import com.mdb.user_data_gateway_service.grpc.GetAdminUsersResponse;
import com.mdb.user_data_gateway_service.grpc.GetUserByKeycloakIdRequest;
import com.mdb.user_data_gateway_service.grpc.UserResponse;
import com.mdb.user_data_gateway_service.grpc.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakJwtAuthenticationConverter.class);

    private final AdminUserServiceGrpc.AdminUserServiceBlockingStub adminUserStub;
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

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
            if (isAdminEmail(email)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                return new JwtAuthenticationToken(jwt, authorities, email);
            }
        }

        // Check if Regular Client User
        if (keycloakId != null) {
            try {
                UserResponse user = userStub.getUserByKeycloakId(
                        GetUserByKeycloakIdRequest.newBuilder()
                                .setKeycloakId(keycloakId)
                                .build()
                );
                if (user != null && !user.getId().isEmpty()) {
                    if (!"ACTIVE".equals(user.getStatus())) {
                        throw new OAuth2AuthenticationException(new OAuth2Error(
                                OAuth2ErrorCodes.ACCESS_DENIED, "User account is not active: " + user.getStatus(), null));
                    }
                    if (user.getRolesCount() > 0) {
                        user.getRolesList().stream()
                                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase()))
                                .forEach(authorities::add);
                    }
                    String username = !user.getUsername().isEmpty() ? user.getUsername() : email;
                    return new JwtAuthenticationToken(jwt, authorities, username);
                }
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() != Status.Code.NOT_FOUND) {
                    throw new OAuth2AuthenticationException(new OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR, "Failed to reach user-data-gateway-service: " + e.getMessage(), null), e);
                }
            }
        }

        // Neither admin nor registered regular user
        throw new OAuth2AuthenticationException(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN, "User is not registered in the local database", null));
    }

    private boolean isAdminEmail(String email) {
        if (email == null) return false;
        try {
            GetAdminUsersResponse response = adminUserStub.getAdminUsers(
                    GetAdminUsersRequest.newBuilder()
                            .setPage(0)
                            .setSize(100)
                            .build()
            );
            return response.getUsersList().stream()
                    .anyMatch(admin -> email.equalsIgnoreCase(admin.getEmail()) && "ACTIVE".equals(admin.getStatus()));
        } catch (Exception e) {
            LOGGER.error("Failed to query admin users list from gateway: {}", e.getMessage());
            return false;
        }
    }
}
