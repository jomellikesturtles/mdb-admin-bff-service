package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem {
    private UUID id;
    private String title;
    private String posterUrl;
    private String status; // DRAFT, PUBLISHED, FLAGGED
    private Integer releaseYear;
}
