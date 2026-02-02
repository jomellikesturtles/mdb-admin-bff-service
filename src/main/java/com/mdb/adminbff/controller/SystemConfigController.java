package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.FeatureFlagRequest;
import com.mdb.adminbff.dto.MaintenanceConfig;
import com.mdb.adminbff.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@Tag(name = "System Configuration", description = "Manage system-wide settings like Maintenance Mode and Feature Flags")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    // --- MAINTENANCE ---

    @GetMapping("/maintenance")
    @Operation(summary = "Get Maintenance Mode Status")
    public ResponseEntity<MaintenanceConfig> getMaintenanceStatus() {
        return ResponseEntity.ok(systemConfigService.getMaintenanceConfig());
    }

    @PutMapping("/maintenance")
    @Operation(summary = "Update Maintenance Mode Status")
    public ResponseEntity<Void> updateMaintenanceStatus(@RequestBody MaintenanceConfig config) {
        systemConfigService.updateMaintenanceConfig(config);
        return ResponseEntity.ok().build();
    }

    // --- FEATURE FLAGS ---

    @GetMapping("/features")
    @Operation(summary = "List all Feature Flags")
    public ResponseEntity<Map<String, Boolean>> getAllFeatureFlags() {
        return ResponseEntity.ok(systemConfigService.getAllFeatureFlags());
    }

    @PutMapping("/features/{key}")
    @Operation(summary = "Toggle a Feature Flag")
    public ResponseEntity<Void> setFeatureFlag(
            @PathVariable String key,
            @RequestBody FeatureFlagRequest request) {
        systemConfigService.setFeatureFlag(key, request);
        return ResponseEntity.ok().build();
    }
}
