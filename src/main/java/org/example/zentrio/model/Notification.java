package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.dto.response.AppUserResponse;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Notification {
    private UUID notificationId;
    private String title;
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private UUID boardId;
    private Board board;
    private UUID taskId;
    private Task task;
    private UUID checklistId;
    private UUID senderId;
    private AppUserResponse sender;
    private UUID receiverId;

}
