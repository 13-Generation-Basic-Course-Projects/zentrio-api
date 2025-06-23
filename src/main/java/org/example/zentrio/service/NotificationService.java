package org.example.zentrio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.zentrio.dto.request.NotificationRequest;
import org.example.zentrio.enums.NotificationTypes;
import org.example.zentrio.model.Notification;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void sendMessageToAllUsers(NotificationRequest notificationRequest) throws JsonProcessingException;

    void sendPushNotificationToUser(NotificationRequest notificationRequest) throws JsonProcessingException;

    HashSet<Notification> getNotificationsForUser(UUID userId);

    void deleteNotificationByUserId(UUID notificationId);

    void deleteAllNotifications();

    void sendPushNotificationToUser(UUID receiverId, String title, String message,UUID boardId, UUID taskId, UUID checklistId, NotificationTypes type) throws JsonProcessingException;

    HashSet<Notification> getNotificationByUserIdAndBoardId(UUID userId, UUID boardId);
}
