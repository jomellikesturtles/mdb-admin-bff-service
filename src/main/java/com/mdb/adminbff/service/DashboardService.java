package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.DashboardStats;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class DashboardService {

    @Async
    public DashboardStats getDashboardStats() {
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

        CompletableFuture<DashboardStats> futures =  CompletableFuture.allOf(userCountFuture, activeUsersFuture, movieCountFuture, pendingJobsFuture)
                .thenApply(v -> DashboardStats.builder()
                        .totalUsers(userCountFuture.join())
                        .activeUsers24h(activeUsersFuture.join())
                        .totalMovies(movieCountFuture.join())
                        .pendingCrawlJobs(pendingJobsFuture.join())
                        .systemHealth("HEALTHY")
                        .build());
		try {
			return futures.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

    private void simulateLatency() {
        int min = 30;
        int max = 8000;
        System.out.println("SLEEP LATENCY" + (int) (Math.random() * (max - min) + min));
        try {
            Thread.sleep((int) (int) (Math.random() * (max - min) + min));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
