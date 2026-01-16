package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerJob {
    private UUID jobId;
    private LocalDateTime startTime;
    private String status; // RUNNING, COMPLETED, FAILED
    private Integer itemsFound;
}
