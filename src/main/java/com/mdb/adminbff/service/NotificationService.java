package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendNotification(NotificationRequest request) {
        // In a real app, we'd serialize the object. For simplicity, we send a string.
        String message = String.format("Title: %s, Message: %s, Channel: %s", 
                request.getTitle(), request.getMessage(), request.getChannel());
        
        log.info("Sending notification to system-alerts: {}", message);
        kafkaTemplate.send("system-alerts", message);
    }
}
