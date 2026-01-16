package com.mdb.adminbff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    @NotBlank
    private String channel; // EMAIL, PUSH, IN_APP
    private String targetGroup; // ALL_USERS, PREMIUM_USERS, ADMINS
}
