package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.CrawlerJob;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CrawlerService {

    private static final List<CrawlerJob> MOCK_JOBS = new ArrayList<>();

    static {
        MOCK_JOBS.add(CrawlerJob.builder()
                .jobId(UUID.randomUUID())
                .startTime(LocalDateTime.now().minusHours(2))
                .status("COMPLETED")
                .itemsFound(150)
                .build());
    }

    public List<CrawlerJob> getJobHistory() {
        return new ArrayList<>(MOCK_JOBS);
    }

    public CompletableFuture<String> triggerCrawl(String targetUrl, Integer depth) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate triggering job
            UUID jobId = UUID.randomUUID();
            MOCK_JOBS.add(0, CrawlerJob.builder()
                    .jobId(jobId)
                    .startTime(LocalDateTime.now())
                    .status("RUNNING")
                    .itemsFound(0)
                    .build());
            return jobId.toString();
        });
    }
}
