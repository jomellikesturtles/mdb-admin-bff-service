package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.User;
import com.mdb.user_data_gateway_service.grpc.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    @Cacheable(value = "users", key = "#page + '-' + #limit + '-' + #search")
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallbackGetUsers")
    public PagedResponse<User> getUsers(int page, int limit, String search) {
        logger.debug("Fetching users via gRPC. Page: {}, Limit: {}, Search: {}", page, limit, search);
        
        GetUsersRequest request = GetUsersRequest.newBuilder()
                .setPage(page)
                .setLimit(limit)
                .setSearch(search != null ? search : "")
                .build();
        
        GetUsersResponse response = userStub.getUsers(request);

        List<User> items = response.getUsersList().stream()
                .map(this::mapGrpcToDto)
                .collect(Collectors.toList());

        logger.info("Fetched {} users out of {}", items.size(), response.getTotalCount());
        return PagedResponse.<User>builder()
                .page(page)
                .limit(limit)
                .totalDocs((int) response.getTotalCount())
                .totalPages(response.getTotalPages())
                .items(items)
                .build();
    }

    @Cacheable(value = "user", key = "#id")
    public Optional<User> getUserById(UUID id) {
        logger.debug("Fetching user by ID via gRPC: {}", id);
        try {
            UserResponse response = userStub.getUserById(
                    UserRequest.newBuilder()
                            .setId(id.toString())
                            .build()
            );
            return Optional.of(mapGrpcToDto(response));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Fetching user by username via gRPC: {}", username);
        try {
            UserResponse response = userStub.getUserByUsername(
                    UserRequest.newBuilder()
                            .setUsername(username)
                            .build()
            );
            return Optional.of(mapGrpcToDto(response));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public User saveUser(User userDto) {
        logger.info("Saving user via gRPC: {}", userDto.getUsername());
        SaveUserRequest request = SaveUserRequest.newBuilder()
                .setId(userDto.getId() != null ? userDto.getId().toString() : "")
                .setKeycloakId(userDto.getKeycloakId() != null ? userDto.getKeycloakId() : "")
                .setUsername(userDto.getUsername() != null ? userDto.getUsername() : "")
                .setEmail(userDto.getEmail() != null ? userDto.getEmail() : "")
                .setStatus(userDto.getStatus() != null ? userDto.getStatus() : "")
                .addAllRoles(userDto.getRoles() != null ? userDto.getRoles() : List.of())
                .build();

        UserResponse response = userStub.saveUser(request);
        return mapGrpcToDto(response);
    }

    @CacheEvict(value = {"user", "users"}, allEntries = true)
    public void banUser(UUID id, String status, String reason) {
        logger.warn("Banning user ID via gRPC: {}. Status: {}. Reason: {}", id, status, reason);
        UpdateUserStatusRequest request = UpdateUserStatusRequest.newBuilder()
                .setId(id.toString())
                .setStatus(status)
                .setReason(reason != null ? reason : "")
                .build();
        userStub.updateUserStatus(request);
    }

    public PagedResponse<User> fallbackGetUsers(int page, int limit, String search, Throwable t) {
        logger.error("Fallback for getUsers triggered. Error: {}", t.getMessage(), t);
        return PagedResponse.<User>builder()
                .page(page)
                .limit(limit)
                .totalDocs(0)
                .totalPages(0)
                .items(new ArrayList<>())
                .build();
    }

    private User mapGrpcToDto(UserResponse entity) {
        return User.builder()
                .id(entity.getId().isEmpty() ? null : UUID.fromString(entity.getId()))
                .keycloakId(entity.getKeycloakId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .roles(entity.getRolesList() != null ? new ArrayList<>(entity.getRolesList()) : new ArrayList<>())
                .lastLogin(parseLocalDateTime(entity.getLastLogin()))
                .build();
    }

    private LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            logger.warn("Failed to parse LocalDateTime: {}", value);
            return null;
        }
    }
}
