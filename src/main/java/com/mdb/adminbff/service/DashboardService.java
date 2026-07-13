package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.DashboardStats;
import com.mdb.media_data_gateway_service.grpc.CountMediaItemsRequest;
import com.mdb.media_data_gateway_service.grpc.MediaServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.CountUsersRequest;
import com.mdb.user_data_gateway_service.grpc.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DashboardService.class);

    private final UserServiceGrpc.UserServiceBlockingStub userStub;
    private final MediaServiceGrpc.MediaServiceBlockingStub mediaItemStub;

    public DashboardStats getDashboardStats() {
        long totalUsers = 0;
        try {
            totalUsers = userStub.countUsers(CountUsersRequest.getDefaultInstance()).getCount();
        } catch (Exception e) {
            logger.warn("Failed to retrieve user count from user-data-gateway-service: {}", e.getMessage());
        }

        long totalMovies = 0;
        try {
            totalMovies = mediaItemStub.countMediaItems(CountMediaItemsRequest.getDefaultInstance()).getCount();
        } catch (Exception e) {
            logger.warn("Failed to retrieve media item count from media-data-gateway-service: {}", e.getMessage());
        }
        
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
