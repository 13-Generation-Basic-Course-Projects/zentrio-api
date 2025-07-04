package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.AnnouncementRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Announcement;
import org.example.zentrio.service.AnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Announcement Controller")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    @Operation(summary = "Create an announcement by board ID")
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest announcementRequest) {

        Announcement created = announcementService.createAnnouncement(announcementRequest);

        // 🔥 Broadcast creation
        messagingTemplate.convertAndSend(
                "/topic/board/" + created.getBoardId() + "/announcements",
                Map.of("type", "CREATED", "announcement", created)
        );


        ApiResponse<Announcement> response = ApiResponse.<Announcement>builder()
                .success(true)
                .message("Created announcement successfully")
                .payload(created)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{announcement-id}")
    @Operation(summary = "Get an announcement by ID")
    public ResponseEntity<ApiResponse<Announcement>> getAnnouncementById(
            @PathVariable("announcement-id") UUID announcementId) {

        Announcement announcement = announcementService.getAnnouncementById(announcementId);

        ApiResponse<Announcement> response = ApiResponse.<Announcement>builder()
                .success(true)
                .message("Retrieved announcement successfully")
                .payload(announcement)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{announcement-id}")
    @Operation(summary = "Update an announcement by ID")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncementById(
            @PathVariable("announcement-id") UUID announcementId,
            @Valid @RequestBody AnnouncementRequest announcementRequest) {

        Announcement updated = announcementService.updateAnnouncementById(announcementId, announcementRequest);

        // 🔥 Broadcast update
        messagingTemplate.convertAndSend(
                "/topic/board/" + updated.getBoardId() + "/announcements",
                Map.of("type", "UPDATED", "announcement", updated)
        );

        ApiResponse<Announcement> response = ApiResponse.<Announcement>builder()
                .success(true)
                .message("Updated announcement successfully")
                .payload(updated)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{announcement-id}/pinned")
    @Operation(summary = "Toggle pin for an announcement by ID")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncementPinnedById(
            @PathVariable("announcement-id") UUID announcementId) {

        Announcement pinned = announcementService.updateAnnouncementPinnedById(announcementId);

        messagingTemplate.convertAndSend(
                "/topic/board/" + pinned.getBoardId() + "/announcements",
                Map.of("type", "UPDATED", "announcement", pinned)
        );

        ApiResponse<Announcement> response = ApiResponse.<Announcement>builder()
                .success(true)
                .message("Toggled pin status for announcement")
                .payload(pinned)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{announcement-id}")
    @Operation(summary = "Delete an announcement by ID")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncementById(
            @PathVariable("announcement-id") UUID announcementId) {

        Announcement announcement = announcementService.getAnnouncementById(announcementId); // Make sure to implement this
        announcementService.deletedAnnouncementById(announcementId);

        // 🔥 Broadcast deletion
        messagingTemplate.convertAndSend(
                "/topic/board/" + announcement.getBoardId() + "/announcements",
                Map.of("type", "DELETED", "announcementId", announcementId)
        );

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Deleted announcement successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/boards/{board-id}")
    @Operation(summary = "Get all announcements by board ID")
    public ResponseEntity<ApiResponse<List<Announcement>>> getAnnouncementsByBoardId(
            @PathVariable("board-id") UUID boardId) {

        List<Announcement> announcements = announcementService.getAnnouncementsByBoardId(boardId);

        ApiResponse<List<Announcement>> response = ApiResponse.<List<Announcement>>builder()
                .success(true)
                .message("Retrieved announcements by board ID")
                .payload(announcements)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    // 🔧 Inner class to represent WebSocket deletion payload (you can move it elsewhere)
    record DeletionPayload(UUID announcementId, String type) {}
}
