package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.request.ReactRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Announcement;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.model.React;
import org.example.zentrio.service.AnnouncementService;
import org.example.zentrio.service.ReactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reacts")
@RequiredArgsConstructor
public class ReactController {

    private final ReactService reactService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<ApiResponse<React>> createReact(@Valid @RequestBody ReactRequest reactRequest) {
        React createdReact = reactService.createReact(reactRequest);

        Announcement updatedAnnouncement = announcementService.getAnnouncementById(createdReact.getAnnouncementId());

        // Broadcast updated announcement with "UPDATED" type
        messagingTemplate.convertAndSend(
                "/topic/board/" + updatedAnnouncement.getBoardId() + "/announcements",
                Map.of("type", "UPDATED", "announcement", updatedAnnouncement)
        );

        ApiResponse<React> apiResponse = ApiResponse.<React>builder()
                .success(true)
                .message("Created React by announcement id successfully")
                .payload(createdReact)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PutMapping("/{react-id}")
    public ResponseEntity<ApiResponse<React>> updateReact(
            @PathVariable("react-id") UUID reactId,
            @Valid @RequestBody ReactRequest reactRequest) {

        React updatedReact = reactService.UpdateReact(reactId, reactRequest);
        Announcement updatedAnnouncement = announcementService.getAnnouncementById(updatedReact.getAnnouncementId());

        // Broadcast updated announcement with "UPDATED" type
        messagingTemplate.convertAndSend(
                "/topic/board/" + updatedAnnouncement.getBoardId() + "/announcements",
                Map.of("type", "UPDATED", "announcement", updatedAnnouncement)
        );

        ApiResponse<React> apiResponse = ApiResponse.<React>builder()
                .success(true)
                .message("Updated React by announcement id successfully")
                .payload(updatedReact)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{react-id}")
    public ResponseEntity<ApiResponse<Void>> deleteReactById(@PathVariable("react-id") UUID reactId) {
        React deletedReact = reactService.GetReactById(reactId);
        reactService.DeleteReactById(reactId);

        Announcement updatedAnnouncement = announcementService.getAnnouncementById(deletedReact.getAnnouncementId());

        // Broadcast updated announcement with "UPDATED" type
        messagingTemplate.convertAndSend(
                "/topic/board/" + updatedAnnouncement.getBoardId() + "/announcements",
                Map.of("type", "UPDATED", "announcement", updatedAnnouncement)
        );

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(true)
                .message("Deleted react by id successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
