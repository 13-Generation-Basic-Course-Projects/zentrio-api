package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.AchievementRequest;
import org.example.zentrio.dto.response.AchievementResponse;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.dto.response.BoardSummary;
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

    @Override
    public Achievement createAchievement(AchievementRequest achievementRequest) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return achievementRepository.createAchievement(achievementRequest, userId);
    }

    @Override
    public Achievement updateAchievement(AchievementRequest achievementRequest) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return achievementRepository.updateAchievement(achievementRequest,userId,LocalDateTime.now());
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
        achievementRepository.deletedAchievement(achievementId,userId);
    }

    @Override
    public AchievementResponse getAchievementByUserId(UUID userId) {
        List<BoardSummary> ownedBoards = achievementRepository.getOwnBoards(userId);
        List<BoardSummary> joinedBoards = achievementRepository.getJoinBoard(userId);
        AppUserResponse user = appUserRepository.getUserByUserId(userId);
        return new AchievementResponse(user, ownedBoards, joinedBoards);
    }

    @Override
    public Map<String, Object> getMyAchievement() {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        List<Workspace> workspaces = workspaceRepository.getAllWorkspacesByUserId(userId);
        Map<String, Object> achievementData = new HashMap<>();

        for (Workspace workspace : workspaces) {
            List<Board> boards = boardRepository.getAllBoardByWorkspaceId(workspace.getWorkspaceId());

            for (Board board : boards) {
                List<Task> tasks = taskRepository.getAllTasksByBoardId(board.getBoardId());
                System.out.println(tasks);
                long completed = tasks.stream()
                        .filter(task -> task.getStage().equals(Stage.COMPLETED.toString()))
                        .count();
                long total = tasks.size();
                double percent = total == 0 ? 0 : (completed * 100.0 / total);

                Map<String, Object> boardData = new HashMap<>();
                boardData.put("boardName", board.getTitle());
                boardData.put("completedTasks", completed);
                boardData.put("totalTasks", total);
                boardData.put("completionRate", String.format("%.0f%%", percent));

                achievementData.put(board.getTitle(), boardData);
            }
        }
        return achievementData;
    }


}
