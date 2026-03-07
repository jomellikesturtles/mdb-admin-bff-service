package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String keycloakId;
    private String username;
    private String email;
    private String status; // ACTIVE, BANNED, SUSPENDED
    private List<String> roles;
    private LocalDateTime lastLogin;
}
