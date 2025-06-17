package org.example.zentrio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Notification;
import org.example.zentrio.model.Task;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.repository.NotificationRepository;
import org.example.zentrio.repository.TaskRepository;
import org.example.zentrio.service.NotificationService;
import org.example.zentrio.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AppUserRepository appUserRepository;
    private final TaskRepository taskRepository;
    @Value("${onesignal.app-id}")
    private String APP_ID;

    @Value("${onesignal.rest-api-key}")
    private String REST_API_KEY;


    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationRepository notificationRepository;


    public void sendMessageToAllUsers(String message) throws JsonProcessingException {
        String url = "https://onesignal.com/api/v1/notifications";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Basic " + REST_API_KEY);

        String strJsonBody = buildJsonBodyForAllUsers(message);
        HttpEntity<String> request = new HttpEntity<>(strJsonBody, headers);
        restTemplate.postForEntity(url, request, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send notification to OneSignal");
        }

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setContent(message);
        notification.setType("push");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        notification.setSenderId(userId);
//        notificationRepository.insertNotification(notification);
    }

    public void sendPushNotificationToUser(String receiverId,UUID taskId, String message) throws JsonProcessingException {

        Task task = taskRepository.getTaskByTaskId(taskId);
        if (task == null) {
            throw new NotFoundException("Task assign ID " +taskId+ "not found");
        }

        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        UUID senderUUID;
        UUID receiverUUID;

        try {
            senderUUID = (userId);
            receiverUUID = UUID.fromString(receiverId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid UUID format for sender or receiver ID");
        }

        AppUser receiverUser = appUserRepository.getUserById(receiverUUID);
        if (receiverUser == null || receiverUser.getUserId() == null) {
            throw new BadRequestException("User with ID " + receiverId + " not found");
        }

//        UUID taskAssignId = notificationRepository.getTaskAssignId(taskId);
//        if (taskAssignId == null) {
//            throw new NotFoundException("Task assignment not found for task ID " + taskId );
//        }


        // 1. Send push notification via OneSignal
        String url = "https://onesignal.com/api/v1/notifications";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Basic " + REST_API_KEY);

        String userStrId = receiverUUID.toString();
        String strJsonBody = buildJsonBodyForSingleUser(message, userStrId);

        HttpEntity<String> request = new HttpEntity<>(strJsonBody, headers);
        restTemplate.postForEntity(url, request, String.class);

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        String content = message;
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }

        System.out.println("content" + content);
        notification.setContent(content);
        notification.setType("IN APP");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setSenderId(senderUUID);
        notification.setReceiverId(receiverUUID);
//        notification.setTaskAssignId(taskAssignId);
        notificationRepository.insertNotification(notification);
        System.out.println("notification: " + notification);

    }

    @Override
    public HashSet<Notification> getNotificationsForUser(UUID userId) {
        AppUser user = appUserRepository.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("User with ID " + userId + " not found");
        }
        return notificationRepository.getNotificationsByUserId(userId);
    }

    @Override
    public void deleteNotificationByUserId(UUID notificationId) {
       Notification notification = notificationRepository.getNotificationById(notificationId);
       if (notification == null) {
           throw new NotFoundException("Notification with ID " + notificationId + " not found");
       }
       UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
       notificationRepository.deleteNotificationByIdAndByUserId(notificationId,userId);
    }

    @Override
    public void deleteAllNotifications() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        notificationRepository.deleteAllNotifications(userId);
    }

    private String buildJsonBodyForAllUsers(String message) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", APP_ID);
        body.put("included_segments", new String[]{"All"});
        Map<String, String> data = new HashMap<>();
        data.put("foo", "bar");
        body.put("data", data);
        Map<String, String> contents = new HashMap<>();
        contents.put("en", message);
        body.put("contents", contents);

        return objectMapper.writeValueAsString(body);
    }

    private String buildJsonBodyForSingleUser(String message, String userId) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", APP_ID);
        body.put("target_channel", "push");
        Map<String, String[]> includeAliases = new HashMap<>();
        includeAliases.put("external_id", new String[]{userId});
        body.put("include_aliases", includeAliases);
        Map<String, String> data = new HashMap<>();
        data.put("foo", "bar");
        body.put("data", data);
        Map<String, String> contents = new HashMap<>();
        contents.put("en", message);
        body.put("contents", contents);

        return objectMapper.writeValueAsString(body);
    }

}
