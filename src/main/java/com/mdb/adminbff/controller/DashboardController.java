package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.DashboardStats;
import com.mdb.adminbff.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard & Analytics")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get Global System Stats")
    @GetMapping("/stats")
    public CompletableFuture<DashboardStats> getStats() {
        return dashboardService.getDashboardStats();
    }
}
