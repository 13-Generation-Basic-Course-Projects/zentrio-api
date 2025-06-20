package org.example.zentrio.service;


import org.example.zentrio.dto.request.AchievementRequest;
import org.example.zentrio.dto.response.AchievementResponse;
import org.example.zentrio.model.Achievement;

import java.util.Map;
import java.util.UUID;

public interface AchievementService {
    Achievement createAchievement(AchievementRequest achievementRequest);

    Achievement updateAchievement(AchievementRequest achievementRequest);

    AchievementResponse getAllAchievementByCurrentUser();

    void deleteAchievement(UUID achievementId);

    AchievementResponse getAchievementByUserId(UUID userId);

    Map<String, Object> getMyAchievement();
}
