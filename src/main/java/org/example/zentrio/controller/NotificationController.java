package org.example.zentrio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Select;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Notification;
import org.example.zentrio.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Notification Controller")
public class NotificationController {

    private final NotificationService notificationService;


    @PostMapping("/sendMessageToAllUsers")
    public void sendMessageToAllUsers(@RequestBody String message) throws JsonProcessingException {
        notificationService.sendMessageToAllUsers(message);
    }

    @PostMapping("/sendMessageToUser/users/{receiver-id}/tasks/{task-id}")
    public void sendMessageToUser(
            @PathVariable("receiver-id") String receiverId,
            @PathVariable("task-id") UUID taskId,
            @RequestBody String message) throws JsonProcessingException {
        notificationService.sendPushNotificationToUser(receiverId, taskId, message);
    }


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


}
