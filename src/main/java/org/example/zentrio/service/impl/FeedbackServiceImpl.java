package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.FeedbackRequest;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.enums.Stage;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Feedback;
import org.example.zentrio.model.Task;
import org.example.zentrio.repository.*;
import org.example.zentrio.service.FeedbackService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TaskRepository taskRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;

    private UUID userId(){
        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();

    }

    private void userRole(UUID boardId){
        String role= memberRepository.getRolePMByBoardIdAndUserId(boardId,userId());
        if (role == null){
            throw new NotFoundException("You don't have any role here...");
        }
        System.out.println("role:"+role);
        if (!role.equals(RoleName.ROLE_MANAGER.name())) {
            throw new ForbiddenException("only ROLE_MANAGER allowed...");
        }
    }


    @Override
    public Feedback createFeedback( FeedbackRequest feedbackRequest) {

        Task task= taskRepository.getTaskByTaskId(feedbackRequest.getTaskId());

        if (task==null) {
            throw new NotFoundException("Task not found");
        }
        System.out.println("task:"+task.getStage());
        if ( task.getStage() == null || !task.getStage().equals(Stage.COMPLETED.name())){
        throw new BadRequestException("Task is not completed");}

        UUID taskId=task.getTaskId();
        userRole(task.getBoardId());
        UUID memberId = memberRepository.getPmId(userId(),task.getBoardId());
        if (memberId == null) {
            throw new NotFoundException("You are not member of this board");
        }

        Feedback feedback = feedbackRepository.createFeedback(LocalDateTime.now(),memberId,taskId,feedbackRequest);
        return feedback;
    }


    @Override
    public Set<Feedback> getAllFeedback(UUID taskId) {

        Task task= taskRepository.getTaskByTaskId(taskId);
        if (task==null) {
            throw new NotFoundException("Task not found");
        }


        return new HashSet<>(feedbackRepository.getAllFeedback(taskId));
    }


    @Override
    public Feedback UpdateFeedbackById(UUID feedbackId, FeedbackRequest feedbackRequest) {
        Feedback feedback = feedbackRepository.getFeedbackById(feedbackId,feedbackRequest.getTaskId());
        if (feedback==null) {
            throw new NotFoundException("Feedback not found");
        }
        Task task = taskRepository.getTaskByTaskId(feedback.getTaskId());
        userRole(task.getBoardId());
            return feedbackRepository.UpdateFeedbackById(feedbackId, feedbackRequest);
    }


    @Override
    public Feedback getFeedbackById(UUID feedbackId, UUID taskId) {
        Feedback feedback = feedbackRepository.getFeedbackById(feedbackId, taskId);
        if (feedback==null) {
            throw new NotFoundException("Feedback not found");
        }
        return feedback;
    }


    @Override
    public void deleteFeedbackById(UUID feedbackId, UUID taskId) {
        Feedback feedback = feedbackRepository.getFeedbackById(feedbackId, taskId);
        if (feedback==null) {
            throw new NotFoundException("Feedback not found");
        }
        Task task = taskRepository.getTaskByTaskId(feedback.getTaskId());
        userRole(task.getBoardId());
        feedbackRepository.deleteFeedbackById(feedbackId);
    }
}
