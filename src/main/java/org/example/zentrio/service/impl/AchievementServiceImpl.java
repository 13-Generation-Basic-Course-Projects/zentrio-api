package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.example.zentrio.dto.request.AchievementRequest;
import org.example.zentrio.dto.response.*;
import org.example.zentrio.enums.Stage;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.AchievementService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final AppUserRepository appUserRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final AuthServiceImpl authService;

    @Override
    public Achievement createAchievement(AchievementRequest achievementRequest) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return achievementRepository.createAchievement(achievementRequest, userId);
    }

    @Override
    public Achievement updateAchievement(AchievementRequest achievementRequest) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return achievementRepository.updateAchievement(achievementRequest, userId, LocalDateTime.now());
    }

    @Override
    public AchievementResponse getAllAchievementByCurrentUser() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();

        List<BoardSummary> ownedBoards = achievementRepository.getOwnBoards(userId);
        List<BoardSummary> joinedBoards = achievementRepository.getJoinBoard(userId);
        AppUserResponse user = appUserRepository.getUserByUserId(userId);

        return new AchievementResponse(
                user,
                ownedBoards,
                joinedBoards
        );
    }

    @Override
    public void deleteAchievement(UUID achievementId) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Achievement achievement = achievementRepository.getAchievementById(achievementId);
        if (achievement == null) {
            throw new NotFoundException("Achievement not found");
        }
        achievementRepository.deletedAchievement(achievementId, userId);
    }

    @Override
    public AchievementFullResponse getAchievementByUserId(UUID userId) {
        AppUserResponse user = appUserRepository.getUserByUserId(userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        return buildAchievementResponse(userId);
    }

    @Override
    public AchievementFullResponse getMyAchievement() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return buildAchievementResponse(userId);
    }

    private AchievementFullResponse buildAchievementResponse(UUID userId) {
        Set<Board> boards = boardRepository.getAllBoardsForCurrentUser(userId);
        List<BoardAchievementDTO> boardSummaries = new ArrayList<>();

        int totalTasks = 0;
        int completedTasks = 0;

        for (Board board : boards) {
            UUID boardId = board.getBoardId();
            List<Task> tasks = new ArrayList<>(taskRepository.getTasksByBoardId(boardId));

            int total = tasks.size();
            int completed = (int) tasks.stream()
                    .filter(task -> Stage.COMPLETED.toString().equals(task.getStage()))
                    .count();

            totalTasks += total;
            completedTasks += completed;

            String percent = total == 0 ? "0%" : String.format("%.0f%%", completed * 100.0 / total);

            boardSummaries.add(new BoardAchievementDTO(
                    boardId,
                    board.getTitle(),
                    total,
                    completed,
                    percent
            ));
        }

        Achievement achievement = achievementRepository.getMyAchievement(userId);
        if (achievement == null) {
            throw new NotFoundException("Achievement not found");
        }

        String overallRate = totalTasks == 0 ? "0%" : String.format("%.0f%%", completedTasks * 100.0 / totalTasks);

        return new AchievementFullResponse(
                userId,
                achievement.getAchievementId(),
                boardSummaries,
                totalTasks,
                completedTasks,
                overallRate
        );
    }

}
