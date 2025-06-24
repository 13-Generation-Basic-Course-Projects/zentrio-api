package org.example.zentrio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.NotificationRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Notification;
import org.example.zentrio.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;



import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Notification Controller")
public class NotificationController {

    private final NotificationService notificationService;


    @PostMapping("/sendMessageToAllUsers")
    public void sendMessageToAllUsers(@RequestBody NotificationRequest notificationRequest) throws JsonProcessingException {
        notificationService.sendMessageToAllUsers(notificationRequest);
    }

    @PostMapping("/sendSpecificToUsers")
    public void sendMessageToUser(@RequestBody NotificationRequest notificationRequest) throws JsonProcessingException {
        log.info("Received notification request for users: {}", notificationRequest.getUserIds());
        notificationService.sendPushNotificationToUser(notificationRequest);
    }


    @Operation(summary = "Get all notifications by users ID",description = "Get all notifications by users ID")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<HashSet<Notification>>> getNotificationByUserId(@PathVariable("userId") UUID userId) {
        ApiResponse<HashSet<Notification>> apiResponse = ApiResponse.<HashSet<Notification>>builder()
                .success(true)
                .message("Get all notifications for user successfully")
                .payload(notificationService.getNotificationsForUser(userId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Deleted notification by ID",description = "Deleted notification by ID And current user ID")
    @DeleteMapping("/{notification-id}")
    public ResponseEntity<?> deleteNotificationByUserId(
            @PathVariable("notification-id") UUID notificationId) {
        notificationService.deleteNotificationByUserId(notificationId);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Delete notification by ID successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Delete all notification",description = "Deleted all notification for current users")
    @DeleteMapping
    public ResponseEntity<?> deleteAllNotifications() {
        notificationService.deleteAllNotifications();
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Delete all notification successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get all notification by users ID and boards ID",description = "Get all notification by user ID and board ID")
    @GetMapping("/users/{user-id}/boards/{board-id}")
    public ResponseEntity<ApiResponse<HashSet<Notification>>> getNotificationByUserIdAndBoardId(
            @PathVariable("user-id") UUID userId,
            @PathVariable("board-id") UUID boardId){
        ApiResponse<HashSet<Notification>> apiResponse = ApiResponse.<HashSet<Notification>>builder()
                .success(true)
                .message("Get all notifications for user by user ID and board ID successfully")
                .payload(notificationService.getNotificationByUserIdAndBoardId(userId,boardId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);

    }





}
