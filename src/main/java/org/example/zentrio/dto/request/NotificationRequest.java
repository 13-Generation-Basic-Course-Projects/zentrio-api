package org.example.zentrio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.NotificationTypes;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private List<UUID> userIds;
    private String title;
    private String message;
    private UUID referenceId;
    private NotificationTypes type;


}
