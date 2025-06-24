package org.example.zentrio.controller;

import com.google.api.services.drive.model.File;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.example.zentrio.dto.request.CommentRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Comment;
import org.example.zentrio.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/comment")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Comment Controller")
public class CommentController {

    private final CommentService commentService;


    @PostMapping("/checklist/")
    @Operation(summary = "Create comment by checklist id")
    public ResponseEntity<ApiResponse<Comment>> createComment
            (@Valid @RequestBody CommentRequest commentRequest)
    {
        ApiResponse<Comment>  response= ApiResponse.<Comment>builder()
                .success(true)
                .message("Create comment  Successfully")
                .payload(commentService.createComment(commentRequest))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{comment-id}")
    @Operation(summary = "Get comment by comment id")
    public ResponseEntity<ApiResponse<Comment>> getCommentByCommentId
            (@PathVariable("comment-id") UUID commentId)
    {
        ApiResponse<Comment>  response= ApiResponse.<Comment>builder()
                .success(true)
                .message("Get comment by comment id successfully")
                .payload(commentService.getCommentByCommentId(commentId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }



    @GetMapping("/checklists/{checklist-id}")
    @Operation(summary = "Get all comments by checklist id")
    public ResponseEntity<ApiResponse<Set<Comment>>> getAllComments(@PathVariable("checklist-id") UUID checkListId)
    {
        ApiResponse<Set<Comment>>  response= ApiResponse.<Set<Comment>>builder()
                .success(true)
                .message("Get all comments by checklist id successfully")
                .payload(commentService.getAllComments(checkListId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{comment-id}")
    @Operation(summary = "Delete  comment by comment id")
    public ResponseEntity<ApiResponse<Void>> deleteCommentByCommentId(@PathVariable("comment-id") UUID commentId)
    {
        commentService.deleteCommentByCommentId(commentId);
        ApiResponse<Void>  response= ApiResponse.<Void>builder()
                .success(true)
                .message("Delete  comment by comment id successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{comment-id}")
    @Operation(summary = "Update  comment by comment id")
    public ResponseEntity<ApiResponse<Comment>> UpdateCommentByCommentId(
            @PathVariable("comment-id") UUID commentId, @Valid @RequestBody CommentRequest commentRequest)
    {
        ApiResponse<Comment>  response= ApiResponse.<Comment>builder()
                .success(true)
                .message("Update comment by comment id successfully")
                .payload(commentService.UpdateCommentByCommentId(commentId,commentRequest))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }




}
