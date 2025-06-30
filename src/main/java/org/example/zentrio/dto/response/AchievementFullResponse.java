package org.example.zentrio.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AchievementFullResponse {
    private UUID userId;
    private UUID achievementId;
    private List<BoardAchievementDTO> boards;
    private int totalTasks;
    private int completedTasks;
    private String completionRate;
}

