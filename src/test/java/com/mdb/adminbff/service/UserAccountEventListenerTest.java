package com.mdb.adminbff.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private UserAccountEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new UserAccountEventListener(messagingTemplate);
    }

    @Test
    void handleUserAccountCreated_shouldSendWebSocketNotification() {
        // Given
        String message = "{\"userId\":\"12345\",\"username\":\"testuser\"}";

        // When
        eventListener.handleUserAccountCreated(message);

        // Then
        verify(messagingTemplate).convertAndSend("/topic/user-created", message);
    }
}
