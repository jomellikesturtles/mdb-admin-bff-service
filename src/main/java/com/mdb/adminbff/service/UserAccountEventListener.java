package com.mdb.adminbff.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "user.account.created", groupId = "mdb-admin-group")
    public void handleUserAccountCreated(String message) {
        log.info("Received user.account.created event: {}", message);
        messagingTemplate.convertAndSend("/topic/user-created", message);
    }
}
