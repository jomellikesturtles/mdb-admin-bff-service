package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.User;
import com.mdb.adminbff.entity.UserEntity;
import com.mdb.adminbff.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

@Cacheable(value = "users", key = "#page + '-' + #limit + '-' + #search")
@CircuitBreaker(name = "externalService", fallbackMethod = "fallbackGetUsers")
@Transactional(readOnly = true)
public PagedResponse<User> getUsers(int page, int limit, String search) {
        logger.debug("Fetching users. Page: {}, Limit: {}, Search: {}", page, limit, search);
        
        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<UserEntity> userPage;
        
        if (search != null && !search.isEmpty()) {
            userPage = userRepository.findByUsernameContainingOrEmailContaining(search, search, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }

        List<User> items = userPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        logger.info("Fetched {} users out of {}", items.size(), userPage.getTotalElements());
        return PagedResponse.<User>builder()
                .page(page)
                .limit(limit)
                .totalDocs((int) userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .items(items)
                .build();
    }

    @Cacheable(value = "user", key = "#id")
    public Optional<User> getUserById(UUID id) {
        logger.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id).map(this::mapToDto);
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username).map(this::mapToDto);
    }

    @Transactional
    public User saveUser(User userDto) {
        logger.info("Saving user: {}", userDto.getUsername());
        UserEntity entity = mapToEntity(userDto);
        UserEntity saved = userRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    public void banUser(UUID id, String status, String reason) {
        logger.warn("Banning user ID: {}. Status: {}. Reason: {}", id, status, reason);
        userRepository.findById(id).ifPresent(u -> {
            u.setStatus(status);
            userRepository.save(u);
            logger.info("User {} status updated to {}", u.getUsername(), status);
        });
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

    private User mapToDto(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .keycloakId(entity.getKeycloakId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .roles(entity.getRoles() != null ? new ArrayList<>(entity.getRoles()) : new ArrayList<>())
                .lastLogin(entity.getLastLogin())
                .build();
    }

    private UserEntity mapToEntity(User userDto) {
        return UserEntity.builder()
                .id(userDto.getId())
                .keycloakId(userDto.getKeycloakId())
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .status(userDto.getStatus())
                .roles(userDto.getRoles() != null ? new ArrayList<>(userDto.getRoles()) : new ArrayList<>())
                .lastLogin(userDto.getLastLogin())
                .build();
    }
}
