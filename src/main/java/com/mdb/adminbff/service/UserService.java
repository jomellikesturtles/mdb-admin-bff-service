package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final List<User> MOCK_USERS = new ArrayList<>();

    static {
        for (int i = 0; i < 50; i++) {
            List<String> roles = new ArrayList<>();
            roles.add("ROLE_USER");
            if (i % 5 == 0) { // Every 5th user is an admin
                roles.add("ROLE_ADMIN");
            }
            
            MOCK_USERS.add(User.builder()
                    .id(UUID.randomUUID())
                    .keycloakId(UUID.randomUUID().toString())
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .status("ACTIVE")
                    .roles(roles)
                    .lastLogin(LocalDateTime.now().minusDays(i))
                    .build());
        }
    }

    @Cacheable(value = "users", key = "#page + '-' + #limit + '-' + #search")
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallbackGetUsers")
    public PagedResponse<User> getUsers(int page, int limit, String search) {
        logger.debug("Fetching users. Page: {}, Limit: {}, Search: {}", page, limit, search);
        simulateLatency();
        
        List<User> filtered = MOCK_USERS.stream()
                .filter(u -> search == null || u.getUsername().contains(search) || u.getEmail().contains(search))
                .collect(Collectors.toList());

        int totalDocs = filtered.size();
        int totalPages = (int) Math.ceil((double) totalDocs / limit);
        int start = Math.min((page - 1) * limit, totalDocs);
        int end = Math.min(start + limit, totalDocs);

        List<User> pagedItems = filtered.subList(start, end);

        logger.info("Fetched {} users out of {}", pagedItems.size(), totalDocs);
        return PagedResponse.<User>builder()
                .page(page)
                .limit(limit)
                .totalDocs(totalDocs)
                .totalPages(totalPages)
                .items(pagedItems)
                .build();
    }

    @Cacheable(value = "user", key = "#id")
    public Optional<User> getUserById(UUID id) {
        logger.debug("Fetching user by ID: {}", id);
        return MOCK_USERS.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Fetching user by username: {}", username);
        return MOCK_USERS.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    public User saveUser(User user) {
        logger.info("Saving user: {}", user.getUsername());
        MOCK_USERS.add(user);
        return user;
    }

    @CacheEvict(value = {"user", "users"}, allEntries = true)
    public void banUser(UUID id, String status, String reason) {
        logger.warn("Banning user ID: {}. Status: {}. Reason: {}", id, status, reason);
        MOCK_USERS.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .ifPresent(u -> {
                    u.setStatus(status);
                    logger.info("User {} status updated to {}", u.getUsername(), status);
                });
    }

    public PagedResponse<User> fallbackGetUsers(int page, int limit, String search, Throwable t) {
        logger.error("Fallback for getUsers triggered. Error: {}", t.getMessage());
        return PagedResponse.<User>builder()
                .page(page)
                .limit(limit)
                .totalDocs(0)
                .totalPages(0)
                .items(List.of())
                .build();
    }

    private void simulateLatency() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
