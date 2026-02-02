package com.mdb.adminbff.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.FeatureFlagRequest;
import com.mdb.adminbff.dto.MaintenanceConfig;
import com.mdb.adminbff.dto.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_MAINTENANCE = "system:config:maintenance";
    private static final String KEY_FEATURES = "system:config:features";
    private static final String TOPIC_SYSTEM_ALERTS = "system-alerts";

    // --- MAINTENANCE ---

    public MaintenanceConfig getMaintenanceConfig() {
        String json = redisTemplate.opsForValue().get(KEY_MAINTENANCE);
        if (json == null) {
            return new MaintenanceConfig(false, "System is running normally", null, null);
        }
        try {
            return objectMapper.readValue(json, MaintenanceConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse maintenance config from Redis", e);
            return new MaintenanceConfig(false, "Error retrieving status", null, null);
        }
    }

    public void updateMaintenanceConfig(MaintenanceConfig config) {
        try {
            if (config.getStartTime() == null && config.isEnabled()) {
                config.setStartTime(LocalDateTime.now());
            }
            String json = objectMapper.writeValueAsString(config);
            redisTemplate.opsForValue().set(KEY_MAINTENANCE, json);

            // Broadcast event
            broadcastChange("MAINTENANCE_UPDATE", config);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize maintenance config", e);
        }
    }

    // --- FEATURE FLAGS ---

    public Map<String, Boolean> getAllFeatureFlags() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(KEY_FEATURES);
        return entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> (String) e.getKey(),
                        e -> Boolean.valueOf((String) e.getValue())
                ));
    }

    public void setFeatureFlag(String key, FeatureFlagRequest request) {
        redisTemplate.opsForHash().put(KEY_FEATURES, key, String.valueOf(request.isEnabled()));
        
        // Broadcast event
        broadcastChange("FEATURE_FLAG_UPDATE", Map.of("key", key, "enabled", request.isEnabled()));
    }

    // --- HELPER ---

    private void broadcastChange(String type, Object payload) {
        try {
            SystemEvent event = new SystemEvent(type, payload, LocalDateTime.now());
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_SYSTEM_ALERTS, type, message);
            log.info("Broadcasted system event: {}", type);
        } catch (JsonProcessingException e) {
            log.error("Failed to broadcast system event", e);
        }
    }
}
