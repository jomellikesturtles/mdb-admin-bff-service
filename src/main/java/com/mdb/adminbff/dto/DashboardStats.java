package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Integer totalUsers;
    private Integer activeUsers24h;
    private Integer totalMovies;
    private String systemHealth; // HEALTHY, DEGRADED, DOWN
    private Integer pendingCrawlJobs;
}
