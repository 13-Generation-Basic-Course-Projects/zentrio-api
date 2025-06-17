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
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private UUID taskAssignId;
    private UUID senderId;
    private AppUserResponse sender;
    private UUID receiverId;

}
