package org.example.zentrio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.zentrio.model.Notification;

import java.util.HashSet;
import java.util.UUID;

public interface NotificationService {
    void sendMessageToAllUsers(String message) throws JsonProcessingException;

    void sendMessageToUser(String senderId, String receiverId, UUID taskId, String message) throws JsonProcessingException;

    HashSet<Notification> getNotificationsForUser(UUID userId);
}
