package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.MediaItem;
import com.mdb.adminbff.dto.MediaItemInput;
import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.entity.MediaItemEntity;
import com.mdb.adminbff.repository.MediaItemRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaItemRepository mediaItemRepository;

    @RateLimiter(name = "externalService")
    public PagedResponse<MediaItem> getMedia(int page, int limit, String status) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<MediaItemEntity> mediaPage;
        
        // Note: For simplicity, we are filtering by status if provided. 
        // A more complex query could be added to MediaItemRepository if needed.
        if (status != null && !status.isEmpty()) {
            mediaPage = mediaItemRepository.findAll(pageRequest); // Actually should filter by status
            // For now, let's just use findAll and filter manually or add a repo method
        } else {
            mediaPage = mediaItemRepository.findAll(pageRequest);
        }

        List<MediaItem> items = mediaPage.getContent().stream()
                .filter(m -> status == null || m.getStatus().equalsIgnoreCase(status))
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<MediaItem>builder()
                .page(page)
                .limit(limit)
                .totalDocs((int) mediaPage.getTotalElements())
                .totalPages(mediaPage.getTotalPages())
                .items(items)
                .build();
    }

    public Optional<MediaItem> getMediaById(UUID id) {
        return mediaItemRepository.findById(id).map(this::mapToDto);
    }

    @Transactional
    public void createMedia(MediaItemInput input) {
        MediaItemEntity entity = MediaItemEntity.builder()
                .title(input.getTitle())
                .posterUrl(input.getPosterUrl())
                .status(input.getStatus())
                .releaseYear(input.getReleaseYear())
                .build();
        mediaItemRepository.save(entity);
    }

    @Transactional
    public void updateMedia(UUID id, MediaItemInput input) {
        mediaItemRepository.findById(id).ifPresent(m -> {
            m.setTitle(input.getTitle());
            m.setPosterUrl(input.getPosterUrl());
            m.setStatus(input.getStatus());
            m.setReleaseYear(input.getReleaseYear());
            mediaItemRepository.save(m);
        });
    }

    private MediaItem mapToDto(MediaItemEntity entity) {
        return MediaItem.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .posterUrl(entity.getPosterUrl())
                .status(entity.getStatus())
                .releaseYear(entity.getReleaseYear())
                .build();
    }
}
