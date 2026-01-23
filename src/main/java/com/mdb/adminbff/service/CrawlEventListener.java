package com.mdb.adminbff.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "crawl-failed", groupId = "mdb-admin-group")
    public void handleCrawlFailed(String message) {
        log.info("Received crawl-failed event: {}", message);
        messagingTemplate.convertAndSend("/topic/errors", message);
    }
}
