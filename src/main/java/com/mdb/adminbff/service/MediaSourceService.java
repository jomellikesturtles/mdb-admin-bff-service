package com.mdb.adminbff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.MediaSource;
import com.mdb.adminbff.dto.MediaSourceInput;
import com.mdb.media_data_gateway_service.grpc.GetTorrentsRequest;
import com.mdb.media_data_gateway_service.grpc.GetTorrentsResponse;
import com.mdb.media_data_gateway_service.grpc.MovieTorrent;
import com.mdb.media_data_gateway_service.grpc.TorrentServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaSourceService {

    private static final Logger logger = LoggerFactory.getLogger(MediaSourceService.class);
    private final TorrentServiceGrpc.TorrentServiceBlockingStub mediaSourceStub;
    private final ObjectMapper objectMapper;

    public PagedResponse<MediaSource> getMediaSources(int page, int limit, String search) {
        logger.debug("Fetching media sources from gRPC. Page: {}, Limit: {}, Search: {}", page, limit, search);
        
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
    }

    private MediaSource mapGrpcToDto(MovieTorrent grpcTorrent) {
        try {
            MediaSource source = objectMapper.readValue(grpcTorrent.getDataJson(), MediaSource.class);
            source.setImdbId(grpcTorrent.getImdbId());
            source.setDataJson(grpcTorrent.getDataJson());
            return source;
        } catch (Exception e) {
            logger.error("Critical error mapping gRPC MovieTorrent dataJson to DTO for IMDB ID {}: {}", 
                    grpcTorrent.getImdbId(), e.getMessage());
            return MediaSource.builder()
                    .imdbId(grpcTorrent.getImdbId())
                    .name("Data Mapping Error")
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
                logger.warn("Media source {} not found in gRPC", id);
                return Optional.empty();
            }
            return Optional.ofNullable(mapGrpcToDto(response));
        } catch (Exception e) {
            logger.error("Error fetching media source {} from gRPC: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public MediaSource addMediaSource(MediaSourceInput input) {
        logger.info("Adding new media source via gRPC: {}", input.getName());
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

    public Optional<MediaSource> updateMediaSource(UUID id, MediaSourceInput input) {
        logger.info("Updating media source ID via gRPC: {}", id);
        return getMediaSourceById(id).map(source -> {
            source.setName(input.getName());
            if (input.getSourceHash() != null) source.setSourceHash(input.getSourceHash());
            if (input.getResourceUri() != null) source.setResourceUri(input.getResourceUri());
            if (input.getSize() != null) source.setSize(input.getSize());
            if (input.getStatus() != null) source.setStatus(input.getStatus());
            if (input.getProgress() != null) source.setProgress(input.getProgress());
            
            MovieTorrent response = mediaSourceStub.saveTorrent(mapDtoToGrpc(source));
            return mapGrpcToDto(response);
        });
    }

    public Optional<MediaSource> patchMediaSource(UUID id, MediaSourceInput input) {
        logger.info("Patching media source ID via gRPC: {}", id);
        return getMediaSourceById(id).map(source -> {
            if (input.getName() != null) source.setName(input.getName());
            if (input.getSourceHash() != null) source.setSourceHash(input.getSourceHash());
            if (input.getResourceUri() != null) source.setResourceUri(input.getResourceUri());
            if (input.getSize() != null) source.setSize(input.getSize());
            if (input.getStatus() != null) source.setStatus(input.getStatus());
            if (input.getProgress() != null) source.setProgress(input.getProgress());
            
            MovieTorrent response = mediaSourceStub.saveTorrent(mapDtoToGrpc(source));
            return mapGrpcToDto(response);
        });
    }

    public boolean deleteMediaSource(UUID id) {
        logger.info("Deleting media source ID via gRPC: {}", id);
        try {
            MovieTorrent request = MovieTorrent.newBuilder()
                    .setImdbId(id.toString())
                    .build();
            MovieTorrent response = mediaSourceStub.deleteTorrent(request);
            return response != null;
        } catch (Exception e) {
            logger.error("Error deleting media source {} via gRPC: {}", id, e.getMessage());
            return false;
        }
    }
}
