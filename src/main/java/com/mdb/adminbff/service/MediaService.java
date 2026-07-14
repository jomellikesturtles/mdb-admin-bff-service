package com.mdb.adminbff.service;

import com.mdb.adminbff.dto.MediaItem;
import com.mdb.adminbff.dto.MediaItemInput;
import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.media_data_gateway_service.grpc.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaServiceGrpc.MediaServiceBlockingStub mediaItemStub;

    @RateLimiter(name = "externalService")
    public PagedResponse<MediaItem> getMedia(int page, int limit, String status) {
        GetMediaItemsRequest request = GetMediaItemsRequest.newBuilder()
                .setPage(page)
                .setLimit(limit)
                .setStatus(status != null ? status : "")
                .build();

        GetMediaItemsResponse response = mediaItemStub.getMediaItems(request);

        List<MediaItem> items = response.getItemsList().stream()
                .map(this::mapGrpcToDto)
                .collect(Collectors.toList());

        return PagedResponse.<MediaItem>builder()
                .page(page)
                .limit(limit)
                .totalDocs((int) response.getTotalCount())
                .totalPages(response.getTotalPages())
                .items(items)
                .build();
    }

    public Optional<MediaItem> getMediaById(UUID id) {
        try {
            MediaItemResponse response = mediaItemStub.getMediaItemById(
                    GetMediaItemByIdRequest.newBuilder()
                            .setId(id.toString())
                            .build()
            );
            return Optional.of(mapGrpcToDto(response));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void createMedia(MediaItemInput input) {
        SaveMediaItemRequest request = SaveMediaItemRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTitle(input.getTitle() != null ? input.getTitle() : "")
                .setPosterUrl(input.getPosterUrl() != null ? input.getPosterUrl() : "")
                .setStatus(input.getStatus() != null ? input.getStatus() : "")
                .setReleaseYear(input.getReleaseYear() != null ? input.getReleaseYear() : 0)
                .build();
        mediaItemStub.saveMediaItem(request);
    }

    public void updateMedia(UUID id, MediaItemInput input) {
        SaveMediaItemRequest request = SaveMediaItemRequest.newBuilder()
                .setId(id.toString())
                .setTitle(input.getTitle() != null ? input.getTitle() : "")
                .setPosterUrl(input.getPosterUrl() != null ? input.getPosterUrl() : "")
                .setStatus(input.getStatus() != null ? input.getStatus() : "")
                .setReleaseYear(input.getReleaseYear() != null ? input.getReleaseYear() : 0)
                .build();
        mediaItemStub.saveMediaItem(request);
    }

    private MediaItem mapGrpcToDto(MediaItemResponse entity) {
        return MediaItem.builder()
                .id(entity.getId().isEmpty() ? null : UUID.fromString(entity.getId()))
                .title(entity.getTitle())
                .posterUrl(entity.getPosterUrl())
                .status(entity.getStatus())
                .releaseYear(entity.getReleaseYear())
                .build();
    }
}
