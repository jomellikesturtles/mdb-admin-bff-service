package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.DashboardStats;
import com.mdb.adminbff.repository.MediaItemRepository;
import com.mdb.adminbff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final MediaItemRepository mediaItemRepository;

    public DashboardStats getDashboardStats() {
        // In a real scenario, these could be async or aggregated by a view/service
        long totalUsers = userRepository.count();
        long totalMovies = mediaItemRepository.count();
        
        // Mocking some stats that would come from other metrics
        int activeUsers24h = (int) (totalUsers * 0.15); // Just a mock ratio
        int pendingCrawlJobs = 5; // Should come from CrawlerService

        return DashboardStats.builder()
                .totalUsers((int) totalUsers)
                .activeUsers24h(activeUsers24h)
                .totalMovies((int) totalMovies)
                .pendingCrawlJobs(pendingCrawlJobs)
                .systemHealth("HEALTHY")
                .build();
    }
}
