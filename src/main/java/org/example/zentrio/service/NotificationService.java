package org.example.zentrio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.zentrio.dto.request.NotificationRequest;
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


    Notification sendNotifications(String message, List<UUID> userIds);
}
