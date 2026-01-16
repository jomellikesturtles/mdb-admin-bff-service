package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.DashboardStats;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DashboardService {

    @Async
    public CompletableFuture<DashboardStats> getDashboardStats() {
        CompletableFuture<Integer> userCountFuture = CompletableFuture.supplyAsync(() -> {
            // Simulate call to User Service
            simulateLatency();
            return 1250;
        });

        CompletableFuture<Integer> activeUsersFuture = CompletableFuture.supplyAsync(() -> {
            // Simulate call to User Service analytics
            simulateLatency();
            return 340;
        });

        CompletableFuture<Integer> movieCountFuture = CompletableFuture.supplyAsync(() -> {
            // Simulate call to Media Service
            simulateLatency();
            return 5000;
        });

        CompletableFuture<Integer> pendingJobsFuture = CompletableFuture.supplyAsync(() -> {
            // Simulate call to Crawler Service
            simulateLatency();
            return 5;
        });

        return CompletableFuture.allOf(userCountFuture, activeUsersFuture, movieCountFuture, pendingJobsFuture)
                .thenApply(v -> DashboardStats.builder()
                        .totalUsers(userCountFuture.join())
                        .activeUsers24h(activeUsersFuture.join())
                        .totalMovies(movieCountFuture.join())
                        .pendingCrawlJobs(pendingJobsFuture.join())
                        .systemHealth("HEALTHY")
                        .build());
    }

    private void simulateLatency() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
