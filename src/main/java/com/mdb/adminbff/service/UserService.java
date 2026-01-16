package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    private static final List<User> MOCK_USERS = new ArrayList<>();

    static {
        for (int i = 0; i < 50; i++) {
            MOCK_USERS.add(User.builder()
                    .id(UUID.randomUUID())
                    .keycloakId(UUID.randomUUID().toString())
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .status("ACTIVE")
                    .lastLogin(LocalDateTime.now().minusDays(i))
                    .build());
        }
    }

    @Cacheable(value = "users", key = "#page + '-' + #limit + '-' + #search")
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallbackGetUsers")
    public PagedResponse<User> getUsers(int page, int limit, String search) {
        simulateLatency();
        
        List<User> filtered = MOCK_USERS.stream()
                .filter(u -> search == null || u.getUsername().contains(search) || u.getEmail().contains(search))
                .collect(Collectors.toList());

        int totalDocs = filtered.size();
        int totalPages = (int) Math.ceil((double) totalDocs / limit);
        int start = Math.min((page - 1) * limit, totalDocs);
        int end = Math.min(start + limit, totalDocs);

        List<User> pagedItems = filtered.subList(start, end);

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
        return MOCK_USERS.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    @CacheEvict(value = {"user", "users"}, allEntries = true)
    public void banUser(UUID id, String status, String reason) {
        MOCK_USERS.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .ifPresent(u -> {
                    u.setStatus(status);
                    // Log reason, etc.
                });
    }

    public PagedResponse<User> fallbackGetUsers(int page, int limit, String search, Throwable t) {
        // Fallback implementation, return empty list or cached data if feasible
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
