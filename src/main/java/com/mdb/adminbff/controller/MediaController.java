package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.MediaItem;
import com.mdb.adminbff.dto.MediaItemInput;
import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media Management")
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "Browse Media Catalog")
    @GetMapping
    public ResponseEntity<PagedResponse<MediaItem>> getMedia(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(mediaService.getMedia(page, limit, status));
    }

    @Operation(summary = "Create new Media Item")
    @PostMapping
    public ResponseEntity<Void> createMedia(@RequestBody @Valid MediaItemInput input) {
        mediaService.createMedia(input);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get media details")
    @GetMapping("/{id}")
    public ResponseEntity<MediaItem> getMedia(@PathVariable UUID id) {
        return mediaService.getMediaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update Media Metadata")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMedia(@PathVariable UUID id, @RequestBody @Valid MediaItemInput input) {
        mediaService.updateMedia(id, input);
        return ResponseEntity.ok().build();
    }
}
