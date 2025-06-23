package org.example.zentrio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.NotificationRequest;
import org.example.zentrio.enums.NotificationTypes;
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
import org.springframework.http.MediaType;
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


    public void sendMessageToAllUsers(NotificationRequest notificationRequest) throws JsonProcessingException {
        String url = "https://onesignal.com/api/v1/notifications";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Basic " + REST_API_KEY);

        String strJsonBody = buildJsonBodyForAllUsers(notificationRequest.getTitle(),notificationRequest.getMessage(),notificationRequest.getType());
        HttpEntity<String> request = new HttpEntity<>(strJsonBody, headers);
        restTemplate.postForEntity(url, request, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send notification to OneSignal");
        }

    }

    public void sendPushNotificationToUser(NotificationRequest notificationRequest) throws JsonProcessingException {

        List<UUID> receiverUUIDs = notificationRequest.getUserIds();
        String message = notificationRequest.getMessage();
        UUID senderUUID = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        // 1. Send push notification via OneSignal
        for (UUID receiverUUID : receiverUUIDs) {
            AppUser receiverUser = appUserRepository.getUserById(receiverUUID);
            if (receiverUser == null) {
                throw new NotFoundException("User with ID " + receiverUUID + " not found");
            }
            // Prepare OneSignal push notification
            String url = "https://onesignal.com/api/v1/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Authorization", "Basic " + REST_API_KEY);

            String userStrId = receiverUUID.toString();
            String strJsonBody = buildJsonBodyForSingleUser(notificationRequest, userStrId);

            HttpEntity<String> request = new HttpEntity<>(strJsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to send notification to OneSignal for user " + receiverUUID);
            }

            // 2. Save in-app notification per user
            Notification notification = new Notification();
            notification.setNotificationId(UUID.randomUUID());
            notification.setTitle(notificationRequest.getTitle());
            notification.setContent(message);
            notification.setType(notification.getType());  // Or use enum if available
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setSenderId(senderUUID);
            notification.setReceiverId(receiverUUID);
            notification.setReferenceId(notificationRequest.getReferenceId());

            notificationRepository.insertNotification(notification);
        }

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

    @Override
    public Notification sendNotifications(String message, List<UUID> userIds) {
//
        return null;
    }



    private String stripQuotes(String input) {
        if (input != null && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }




    private String buildJsonBodyForAllUsers(String title, String message, NotificationTypes types) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", APP_ID);
        body.put("included_segments", new String[]{"All"});
        Map<String, String> heading = new HashMap<>();
        heading.put("en", title);
        body.put("headings", heading);
        Map<String, String> contents = new HashMap<>();
        contents.put("en", message);
        body.put("contents", contents);

        Map<String, String> data = new HashMap<>();
        data.put("notificationType", types.name());
        body.put("data", data);
        return objectMapper.writeValueAsString(body);
    }

    private String buildJsonBodyForSingleUser(NotificationRequest notificationRequest, String userId) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", APP_ID);
        body.put("target_channel", "push");
        Map<String, String[]> includeAliases = new HashMap<>();
        includeAliases.put("external_id", new String[]{userId});
        body.put("include_aliases", includeAliases);
        Map<String, String> headings = new HashMap<>();
        headings.put("en", notificationRequest.getTitle() != null && !notificationRequest.getTitle().isEmpty() ? notificationRequest.getTitle() : "Notification");
        body.put("headings", headings);
        Map<String, String> titles = new HashMap<>();
        titles.put("en", notificationRequest.getTitle() != null && !notificationRequest.getTitle().isEmpty() ? notificationRequest.getTitle() : "Notification");
        body.put("title", titles);

        Map<String, String> contents = new HashMap<>();
        contents.put("en",notificationRequest.getMessage() != null && !notificationRequest.getMessage().isEmpty() ? notificationRequest.getMessage() : "Notification");
        body.put("contents", contents);

        return objectMapper.writeValueAsString(body);
    }

}
