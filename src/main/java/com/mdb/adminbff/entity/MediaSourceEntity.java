package com.mdb.adminbff.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_sources", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "imdb_id")
    private String imdbId;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_hash", columnDefinition = "TEXT")
    private String sourceHash;

    @Column(name = "resource_uri", columnDefinition = "TEXT")
    private String resourceUri;

    private Long size; // Size in bytes

    private String status; // SEEDING, DOWNLOADING, COMPLETED, PAUSED, ERROR

    @Column(name = "download_speed")
    private Double downloadSpeed;

    @Column(name = "upload_speed")
    private Double uploadSpeed;

    private Double progress; // 0.0 to 100.0

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
