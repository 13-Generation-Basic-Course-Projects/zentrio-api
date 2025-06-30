package org.example.zentrio.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoardAchievementDTO {
    private UUID boardId;
    private String title;
    private int totalTasks;
    private int completedTasks;
    private String completionRate;
}
