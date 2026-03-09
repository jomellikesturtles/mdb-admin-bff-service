package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaSource implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String imdbId;
    private String name;
    private String sourceHash;
    private String resourceUri;
    private Long size;
    private String status;
    private Double downloadSpeed;
    private Double uploadSpeed;
    private Double progress;
    private String dataJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
