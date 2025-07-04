package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.CommentAnnouncementRequest;
import org.example.zentrio.dto.request.CommentRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.CommentAnnouncementResponse;
import org.example.zentrio.model.Announcement;
import org.example.zentrio.model.Comment;
import org.example.zentrio.service.AnnouncementService;
import org.example.zentrio.service.CommentAnnouncementService;
import org.example.zentrio.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/comment/announcement")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Comment Announcement Controller")
public class CommentAnnouncementController {

    private final CommentAnnouncementService commentAnnouncementService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnnouncementService announcementService;

    @PostMapping("/announcement/")
    @Operation(summary = "Create comment by announcement id")
    public ResponseEntity<ApiResponse<CommentAnnouncementResponse>> createAnnouncementComment(
            @Valid @RequestBody CommentAnnouncementRequest request) {

        CommentAnnouncementResponse createdComment = commentAnnouncementService.createAnnouncementComment(request);

        // Get the announcement to get the boardId (same pattern as announcements)
        Announcement announcement = announcementService.getAnnouncementById(createdComment.getAnnouncementId());

        // DEBUG: Print what you're sending
        Map<String, Object> message = Map.of("type", "CREATED", "comment", createdComment);
        System.out.println("Sending WebSocket message: " + message);

        messagingTemplate.convertAndSend(
                "/topic/board/" + announcement.getBoardId() + "/comments",
                message
        );

        ApiResponse<CommentAnnouncementResponse> response = ApiResponse.<CommentAnnouncementResponse>builder()
                .success(true)
                .message("Created comment successfully")
                .payload(createdComment)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{comment-id}")
    public ResponseEntity<ApiResponse<CommentAnnouncementResponse>> updateComment(
            @PathVariable("comment-id") UUID commentId,
            @Valid @RequestBody CommentAnnouncementRequest request) {

        CommentAnnouncementResponse updatedComment = commentAnnouncementService.UpdateAnnouncementCommentByCommentId(commentId, request);

        // Get the announcement to get the boardId (same pattern as announcements)
        Announcement announcement = announcementService.getAnnouncementById(updatedComment.getAnnouncementId());

        // Send to board-level topic (same pattern as announcements)
        messagingTemplate.convertAndSend(
                "/topic/board/" + announcement.getBoardId() + "/comments",
                Map.of("type", "UPDATED", "comment", updatedComment)
        );

        ApiResponse<CommentAnnouncementResponse> response = ApiResponse.<CommentAnnouncementResponse>builder()
                .success(true)
                .message("Updated comment successfully")
                .payload(updatedComment)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{comment-id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable("comment-id") UUID commentId) {
        // First get the comment to get announcementId before deletion
        CommentAnnouncementResponse commentToDelete = commentAnnouncementService.getCommentByCommentId(commentId);

        // Get the announcement to get the boardId (same pattern as announcements)
        Announcement announcement = announcementService.getAnnouncementById(commentToDelete.getAnnouncementId());

        commentAnnouncementService.deleteAnnouncementCommentByCommentId(commentId);

        // Send to board-level topic (same pattern as announcements)
        messagingTemplate.convertAndSend(
                "/topic/board/" + announcement.getBoardId() + "/comments",
                Map.of("type", "DELETED", "commentId", commentId, "announcementId", commentToDelete.getAnnouncementId())
        );

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Deleted comment successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }
}