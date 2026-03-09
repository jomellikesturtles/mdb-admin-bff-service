package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.MediaSource;
import com.mdb.adminbff.dto.MediaSourceInput;
import com.mdb.adminbff.service.MediaSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/media-sources")
@RequiredArgsConstructor
@Tag(name = "Media Sources", description = "Media Source Management")
public class MediaSourceController {

    private final MediaSourceService mediaSourceService;

    @Operation(summary = "Get Paginated Media Sources")
    @GetMapping
    public PagedResponse<MediaSource> getMediaSources(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search) {
        return mediaSourceService.getMediaSources(page, limit, search);
    }

    @Operation(summary = "Get Media Source by ID")
    @GetMapping("/{id}")
    public ResponseEntity<MediaSource> getMediaSourceById(@PathVariable UUID id) {
        return mediaSourceService.getMediaSourceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Add Media Source Manually")
    @PostMapping
    public ResponseEntity<MediaSource> addMediaSource(@Valid @RequestBody MediaSourceInput input) {
        MediaSource saved = mediaSourceService.addMediaSource(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Update Media Source (Full Update)")
    @PutMapping("/{id}")
    public ResponseEntity<MediaSource> updateMediaSource(@PathVariable UUID id, @Valid @RequestBody MediaSourceInput input) {
        return mediaSourceService.updateMediaSource(id, input)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Patch Media Source (Partial Update)")
    @PatchMapping("/{id}")
    public ResponseEntity<MediaSource> patchMediaSource(@PathVariable UUID id, @RequestBody MediaSourceInput input) {
        return mediaSourceService.patchMediaSource(id, input)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete Media Source")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMediaSource(@PathVariable UUID id) {
        if (mediaSourceService.deleteMediaSource(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
