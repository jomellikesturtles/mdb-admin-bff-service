package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.NotificationRequest;
import com.mdb.adminbff.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification Management")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Send a System-Wide Notification")
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody @Valid NotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.accepted().build();
    }
}
