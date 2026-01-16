package com.mdb.adminbff.dto;

import lombok.Data;

@Data
public class BanUserRequest {
    private String status; // ACTIVE, BANNED, SUSPENDED
    private String reason;
}
