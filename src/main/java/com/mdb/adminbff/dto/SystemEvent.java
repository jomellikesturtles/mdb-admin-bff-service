package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemEvent {
    private String type; // e.g., MAINTENANCE_UPDATE, FEATURE_FLAG_UPDATE
    private Object payload;
    private LocalDateTime timestamp;
}
