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
import org.example.zentrio.model.Comment;
import org.example.zentrio.service.CommentAnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @PostMapping("/announcement/")
    @Operation(summary = "Create comment by announcement id")
    public ResponseEntity<ApiResponse<CommentAnnouncementResponse>> createAnnouncementComment
            (@Valid @RequestBody CommentAnnouncementRequest  commentAnnouncementRequest)
    {

        CommentAnnouncementResponse commentAnnouncementResponse = commentAnnouncementService.createAnnouncementComment(commentAnnouncementRequest);
        // 🔥 Send WebSocket message to /topic/announcement/{announcementId}/comments
        messagingTemplate.convertAndSend(
                "/topic/announcement/" + commentAnnouncementResponse.getAnnouncementId() + "/comments",
                commentAnnouncementResponse
        );
        ApiResponse<CommentAnnouncementResponse>  response= ApiResponse.<CommentAnnouncementResponse>builder()
                .success(true)
                .message("Create comment  Successfully")
                .payload(commentAnnouncementResponse)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(201).body(response);
    }


    @GetMapping("/{comment-id}")
    @Operation(summary = "Get comment by comment id")
    public ResponseEntity<ApiResponse<CommentAnnouncementResponse>> getCommentByCommentId
            (@PathVariable("comment-id") UUID commentId)
    {
        ApiResponse<CommentAnnouncementResponse>  response= ApiResponse.<CommentAnnouncementResponse>builder()
                .success(true)
                .message("Get comment by comment id successfully")
                .payload(commentAnnouncementService.getCommentByCommentId(commentId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }


    @GetMapping("/announcement/{announcement-id}")
    @Operation(summary = "Get all comments by announcement id")
    public ResponseEntity<ApiResponse<Set<CommentAnnouncementResponse>>> getAllAnnouncementComments(@PathVariable("announcement-id") UUID announcementId)
    {
        ApiResponse<Set<CommentAnnouncementResponse>>  response= ApiResponse.<Set<CommentAnnouncementResponse>>builder()
                .success(true)
                .message("Get all comments by announcement id successfully")
                .payload(commentAnnouncementService.getAllAnnouncementComments(announcementId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{comment-id}")
    @Operation(summary = "Delete announcement comment by comment id")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncementCommentByCommentId(@PathVariable("comment-id") UUID commentId)
    {
        commentAnnouncementService.deleteAnnouncementCommentByCommentId(commentId);
        ApiResponse<Void>  response= ApiResponse.<Void>builder()
                .success(true)
                .message("Delete announcement comment by comment id successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }



    @PutMapping("/{comment-id}")
    @Operation(summary = "Update announcement comment by comment id")
    public ResponseEntity<ApiResponse<CommentAnnouncementResponse>> UpdateAnnouncementCommentByCommentId(
            @PathVariable("comment-id") UUID commentId, @Valid @RequestBody CommentAnnouncementRequest commentAnnouncementRequest)
    {
        ApiResponse<CommentAnnouncementResponse>  response= ApiResponse.<CommentAnnouncementResponse>builder()
                .success(true)
                .message("Update announcement comment by comment id successfully")
                .payload(commentAnnouncementService.UpdateAnnouncementCommentByCommentId(commentId,commentAnnouncementRequest))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }



}
