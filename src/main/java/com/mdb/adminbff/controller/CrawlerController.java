package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.CrawlerJob;
import com.mdb.adminbff.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/crawler")
@RequiredArgsConstructor
@Tag(name = "Crawler", description = "Crawler Control")
public class CrawlerController {

    private final CrawlerService crawlerService;

    @Operation(summary = "Get Crawler Job History")
    @GetMapping("/jobs")
    public ResponseEntity<List<CrawlerJob>> getJobs() {
        return ResponseEntity.ok(crawlerService.getJobHistory());
    }

    @Operation(summary = "Trigger a Manual Crawl")
    @PostMapping("/jobs")
    public CompletableFuture<ResponseEntity<Map<String, String>>> triggerCrawl(@RequestBody ManualCrawlRequest request) {
        return crawlerService.triggerCrawl(request.getTargetUrl(), request.getDepth())
                .thenApply(jobId -> ResponseEntity.accepted().body(Map.of(
                        "jobId", jobId,
                        "status", "QUEUED"
                )));
    }

    @Data
    public static class ManualCrawlRequest {
        private String targetUrl;
        private Integer depth;
    }
}
