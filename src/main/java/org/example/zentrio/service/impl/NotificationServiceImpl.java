package org.example.zentrio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.zentrio.dto.request.NotificationRequest;
import org.example.zentrio.enums.NotificationTypes;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AppUserRepository appUserRepository;
    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final ChecklistRepository checklistRepository;
    private final SimpMessagingTemplate messagingTemplate;
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


        Board board = boardRepository.getBoardByBoardId(notificationRequest.getBoardId());
        if (board == null) {
            throw new NotFoundException("Board ID " + notificationRequest.getBoardId() + " not found");
        }


        Task task = null;
        if (notificationRequest.getTaskId() != null) {
            task = taskRepository.getTaskByTaskId(notificationRequest.getTaskId());
            if (task == null) {
                throw new NotFoundException("Task ID " + notificationRequest.getTaskId() + " not found");
            }
        }


        Checklist checklist = null;
        if (notificationRequest.getChecklistId() != null) {
            checklist = checklistRepository.getChecklistById(notificationRequest.getChecklistId());
            if (checklist == null) {
                throw new NotFoundException("Checklist ID " + notificationRequest.getChecklistId() + " not found");
            }
        }

        // 1. Send push notification via OneSignal
        for (UUID receiverUUID : receiverUUIDs) {
            AppUser receiverUser = appUserRepository.getUserById(receiverUUID);
            if (receiverUser == null) {
                throw new NotFoundException("User with ID " + receiverUUID + " not found");
            }

            // Prepare push
            String url = "https://onesignal.com/api/v1/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Authorization", "Basic " + REST_API_KEY);

            String strJsonBody = buildJsonBodyForSingleUser(notificationRequest, receiverUUID.toString());
            HttpEntity<String> request = new HttpEntity<>(strJsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to send notification to OneSignal for user " + receiverUUID);
            }

            // 2. Save in DB
            Notification notification = new Notification();
            notification.setNotificationId(UUID.randomUUID());
            notification.setTitle(notificationRequest.getTitle());
            notification.setContent(message);
            notification.setType(notificationRequest.getType());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setBoardId(notificationRequest.getBoardId());
            notification.setTaskId(notificationRequest.getTaskId()); // nullable
            notification.setChecklistId(notificationRequest.getChecklistId()); // nullable
            notification.setSenderId(senderUUID);
            notification.setReceiverId(receiverUUID);
            notificationRepository.insertNotification(notification);


            log.info("Sending WS notification to user {}: {}", receiverUUID, notification);


            messagingTemplate.convertAndSendToUser(
                    receiverUUID.toString(),       // user destination prefix
                    "/topic/notifications",        // destination (must match frontend subscription)
                    notification                   // the notification object you just saved
            );
            log.info("notification send");

            messagingTemplate.convertAndSend("/topic/all-notifications", notification);
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
    public void sendPushNotificationToUser(UUID receiverId, String title, String message,UUID boardId, UUID taskId, UUID checklistId, NotificationTypes type) throws JsonProcessingException {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setUserIds(List.of(receiverId));
        notificationRequest.setTitle(title);
        notificationRequest.setMessage(message);
        notificationRequest.setType(type.name());
        notificationRequest.setBoardId(boardId);
        notificationRequest.setTaskId(taskId);
        notificationRequest.setChecklistId(checklistId);
        sendPushNotificationToUser(notificationRequest);
    }

    @Override
    public HashSet<Notification> getNotificationByUserIdAndBoardId(UUID userId, UUID boardId) {
        AppUser user = appUserRepository.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("User with ID " + userId + " not found");
        }
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board with ID " + boardId + " not found");
        }

        return notificationRepository.getNotificationByUserIdAndBoardId(userId,boardId);
    }


    private String stripQuotes(String input) {
        if (input != null && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }


    private String buildJsonBodyForAllUsers(String title, String message, String type) throws JsonProcessingException {
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
        data.put("notificationType", type);
        body.put("data", data);
        return objectMapper.writeValueAsString(body);
    }

    private String buildJsonBodyForSingleUser(NotificationRequest notificationRequest, String userId) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", APP_ID);
        body.put("target_channel", "push");
        String templateId = "3ea5a3d9-b2d5-4cae-9890-e579ea279ed3";
        Map<String, String[]> includeAliases = new HashMap<>();
        includeAliases.put("external_id", new String[]{userId});
        body.put("include_aliases", includeAliases);
        body.put("template_id", templateId);
        Map<String, String> headings = new HashMap<>();
        headings.put("en", notificationRequest.getTitle() != null && !notificationRequest.getTitle().isEmpty()
                ? notificationRequest.getTitle()
                : "Notification");
        body.put("headings", headings);

        // Contents (body/message of the notification)
        Map<String, String> contents = new HashMap<>();
        contents.put("en", notificationRequest.getMessage() != null && !notificationRequest.getMessage().isEmpty()
                ? notificationRequest.getMessage()
                : "You have a new notification");
        body.put("contents", contents);

        // Optional: add metadata
        Map<String, String> data = new HashMap<>();
        data.put("type", notificationRequest.getType());
        data.put("boardId",notificationRequest.getBoardId().toString());
        if (notificationRequest.getTaskId() != null) {
            data.put("taskId", notificationRequest.getTaskId().toString());
        }
        if (notificationRequest.getChecklistId() != null) {
            data.put("checklistId", notificationRequest.getChecklistId().toString());
        }
        body.put("data", data);

        return objectMapper.writeValueAsString(body);
    }


}
