package com.mdb.adminbff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.MediaSource;
import com.mdb.adminbff.dto.MediaSourceInput;
import com.mdb.adminbff.entity.MediaSourceEntity;
import com.mdb.adminbff.repository.MediaSourceRepository;
import com.mdb.media_data_gateway_service.grpc.GetTorrentsRequest;
import com.mdb.media_data_gateway_service.grpc.GetTorrentsResponse;
import com.mdb.media_data_gateway_service.grpc.MovieTorrent;
import com.mdb.media_data_gateway_service.grpc.TorrentServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class MediaSourceService {

    private static final Logger logger = LoggerFactory.getLogger(MediaSourceService.class);
    private final MediaSourceRepository mediaSourceRepository;
    private final TorrentServiceGrpc.TorrentServiceBlockingStub mediaSourceStub;
    private final ObjectMapper objectMapper;

    public PagedResponse<MediaSource> getMediaSources(int page, int limit, String search) {
        logger.debug("Fetching media sources from gRPC. Page: {}, Limit: {}, Search: {}", page, limit, search);
        
        try {
            GetTorrentsRequest request = GetTorrentsRequest.newBuilder()
                    .setPageNumber(page)
                    .setPageSize(limit)
                    .build();
            
            GetTorrentsResponse response = mediaSourceStub.getTorrents(request);
            
            List<MediaSource> items = response.getTorrentsList().stream()
                    .map(this::mapGrpcToDto)
                    .collect(Collectors.toList());

            return PagedResponse.<MediaSource>builder()
                    .page(page)
                    .limit(limit)
                    .totalDocs((int) response.getTotalCount())
                    .totalPages((int) Math.ceil((double) response.getTotalCount() / limit))
                    .items(items)
                    .build();
        } catch (Exception e) {
            logger.error("Error fetching media sources from gRPC: {}", e.getMessage());
            return PagedResponse.<MediaSource>builder()
                    .page(page)
                    .limit(limit)
                    .totalDocs(0)
                    .totalPages(0)
                    .items(List.of())
                    .build();
        }
    }

    private MediaSource mapGrpcToDto(MovieTorrent grpcTorrent) {
        try {
            MediaSource source = objectMapper.readValue(grpcTorrent.getDataJson(), MediaSource.class);
            source.setImdbId(grpcTorrent.getImdbId());
            source.setDataJson(grpcTorrent.getDataJson());
            return source;
        } catch (Exception e) {
            logger.error("Error mapping gRPC MovieTorrent to DTO: {}", e.getMessage());
            return MediaSource.builder()
                    .imdbId(grpcTorrent.getImdbId())
                    .name("Unknown (Mapping Error)")
                    .status("ERROR")
                    .dataJson(grpcTorrent.getDataJson())
                    .build();
        }
    }

    public Optional<MediaSource> getMediaSourceById(UUID id) {
        logger.debug("Fetching media source by ID from gRPC: {}", id);
        try {
            MovieTorrent request = MovieTorrent.newBuilder()
                    .setImdbId(id.toString())
                    .build();
            
            MovieTorrent response = mediaSourceStub.getTorrent(request);
            if (response.getDataJson().isEmpty() && response.getImdbId().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(mapGrpcToDto(response));
        } catch (Exception e) {
            logger.error("Error fetching media source {} from gRPC: {}", id, e.getMessage());
            return mediaSourceRepository.findById(id).map(this::mapToDto);
        }
    }

    @Transactional
    public MediaSource addMediaSource(MediaSourceInput input) {
        logger.info("Adding new media source via gRPC: {}", input.getName());
        try {
            MediaSource source = MediaSource.builder()
                    .id(UUID.randomUUID())
                    .imdbId(input.getImdbId())
                    .name(input.getName())
                    .sourceHash(input.getSourceHash())
                    .resourceUri(input.getResourceUri())
                    .size(input.getSize())
                    .status(input.getStatus() != null ? input.getStatus() : "DOWNLOADING")
                    .progress(input.getProgress() != null ? input.getProgress() : 0.0)
                    .downloadSpeed(0.0)
                    .uploadSpeed(0.0)
                    .build();

            MovieTorrent request = mapDtoToGrpc(source);
            MovieTorrent response = mediaSourceStub.saveTorrent(request);
            
            return mapGrpcToDto(response);
        } catch (Exception e) {
            logger.error("Error adding media source via gRPC: {}. Falling back to local repo.", e.getMessage());
            MediaSourceEntity entity = MediaSourceEntity.builder()
                    .name(input.getName())
                    .sourceHash(input.getSourceHash())
                    .resourceUri(input.getResourceUri())
                    .size(input.getSize())
                    .status(input.getStatus() != null ? input.getStatus() : "DOWNLOADING")
                    .progress(input.getProgress() != null ? input.getProgress() : 0.0)
                    .downloadSpeed(0.0)
                    .uploadSpeed(0.0)
                    .build();
            
            MediaSourceEntity saved = mediaSourceRepository.save(entity);
            return mapToDto(saved);
        }
    }

    private MovieTorrent mapDtoToGrpc(MediaSource source) {
        try {
            String json = objectMapper.writeValueAsString(source);
            return MovieTorrent.newBuilder()
                    .setDataJson(json)
                    .setImdbId(source.getImdbId() != null ? source.getImdbId() : "")
                    .build();
        } catch (Exception e) {
            logger.error("Error mapping DTO to gRPC MovieTorrent: {}", e.getMessage());
            return MovieTorrent.getDefaultInstance();
        }
    }

    @Transactional
    public Optional<MediaSource> updateMediaSource(UUID id, MediaSourceInput input) {
        logger.info("Updating media source ID via gRPC: {}", id);
        return getMediaSourceById(id).map(source -> {
            source.setName(input.getName());
            if (input.getSourceHash() != null) source.setSourceHash(input.getSourceHash());
            if (input.getResourceUri() != null) source.setResourceUri(input.getResourceUri());
            if (input.getSize() != null) source.setSize(input.getSize());
            if (input.getStatus() != null) source.setStatus(input.getStatus());
            if (input.getProgress() != null) source.setProgress(input.getProgress());
            
            try {
                MovieTorrent response = mediaSourceStub.saveTorrent(mapDtoToGrpc(source));
                return mapGrpcToDto(response);
            } catch (Exception e) {
                logger.error("Error updating media source {} via gRPC: {}", id, e.getMessage());
                return source;
            }
        });
    }

    @Transactional
    public Optional<MediaSource> patchMediaSource(UUID id, MediaSourceInput input) {
        logger.info("Patching media source ID via gRPC: {}", id);
        return getMediaSourceById(id).map(source -> {
            if (input.getName() != null) source.setName(input.getName());
            if (input.getSourceHash() != null) source.setSourceHash(input.getSourceHash());
            if (input.getResourceUri() != null) source.setResourceUri(input.getResourceUri());
            if (input.getSize() != null) source.setSize(input.getSize());
            if (input.getStatus() != null) source.setStatus(input.getStatus());
            if (input.getProgress() != null) source.setProgress(input.getProgress());
            
            try {
                MovieTorrent response = mediaSourceStub.saveTorrent(mapDtoToGrpc(source));
                return mapGrpcToDto(response);
            } catch (Exception e) {
                logger.error("Error patching media source {} via gRPC: {}", id, e.getMessage());
                return source;
            }
        });
    }

    @Transactional
    public boolean deleteMediaSource(UUID id) {
        if (mediaSourceRepository.existsById(id)) {
            mediaSourceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private MediaSource mapToDto(MediaSourceEntity entity) {
        return MediaSource.builder()
                .id(entity.getId())
                .imdbId(entity.getImdbId())
                .name(entity.getName())
                .sourceHash(entity.getSourceHash())
                .resourceUri(entity.getResourceUri())
                .size(entity.getSize())
                .status(entity.getStatus())
                .downloadSpeed(entity.getDownloadSpeed())
                .uploadSpeed(entity.getUploadSpeed())
                .progress(entity.getProgress())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
