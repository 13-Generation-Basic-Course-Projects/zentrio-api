package org.example.zentrio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private List<UUID> userIds;
    private String title;
    private String message;
    private String type;
    private UUID boardId;
    private UUID taskId;
    private UUID checklistId;


}
