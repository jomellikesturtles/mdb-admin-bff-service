package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.MediaItem;
import com.mdb.adminbff.dto.MediaItemInput;
import com.mdb.adminbff.dto.PagedResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private static final List<MediaItem> MOCK_MEDIA = new ArrayList<>();

    static {
        for (int i = 0; i < 30; i++) {
            MOCK_MEDIA.add(MediaItem.builder()
                    .id(UUID.randomUUID())
                    .title("Movie " + i)
                    .posterUrl("http://example.com/poster" + i + ".jpg")
                    .status(i % 3 == 0 ? "PUBLISHED" : "DRAFT")
                    .releaseYear(2020 + (i % 5))
                    .build());
        }
    }

    @RateLimiter(name = "externalService")
    public PagedResponse<MediaItem> getMedia(int page, int limit, String status) {
        simulateLatency();

        List<MediaItem> filtered = MOCK_MEDIA.stream()
                .filter(m -> status == null || m.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());

        int totalDocs = filtered.size();
        int totalPages = (int) Math.ceil((double) totalDocs / limit);
        int start = Math.min((page - 1) * limit, totalDocs);
        int end = Math.min(start + limit, totalDocs);

        List<MediaItem> pagedItems = filtered.subList(start, end);

        return PagedResponse.<MediaItem>builder()
                .page(page)
                .limit(limit)
                .totalDocs(totalDocs)
                .totalPages(totalPages)
                .items(pagedItems)
                .build();
    }

    public Optional<MediaItem> getMediaById(UUID id) {
        return MOCK_MEDIA.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst();
    }

    public void createMedia(MediaItemInput input) {
        MOCK_MEDIA.add(MediaItem.builder()
                .id(UUID.randomUUID())
                .title(input.getTitle())
                .posterUrl(input.getPosterUrl())
                .status(input.getStatus())
                .releaseYear(input.getReleaseYear())
                .build());
    }

    public void updateMedia(UUID id, MediaItemInput input) {
        MOCK_MEDIA.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .ifPresent(m -> {
                    m.setTitle(input.getTitle());
                    m.setPosterUrl(input.getPosterUrl());
                    m.setStatus(input.getStatus());
                    m.setReleaseYear(input.getReleaseYear());
                });
    }

    private void simulateLatency() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
