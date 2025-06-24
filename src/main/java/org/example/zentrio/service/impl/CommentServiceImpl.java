package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.CommentRequest;
import org.example.zentrio.enums.RoleName;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.*;
import org.example.zentrio.repository.AnnouncementRepository;
import org.example.zentrio.repository.AttachmentRepository;
import org.example.zentrio.repository.CommentRepository;
import org.example.zentrio.repository.MemberRepository;
import org.example.zentrio.service.ChecklistService;
import org.example.zentrio.service.CommentService;
import org.example.zentrio.service.TaskService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ChecklistService checklistService;
    private final TaskService taskService;
    private final MemberRepository memberRepository;




    public UUID userId (){
        AppUser appUser= (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();
    }

    @Override
    public Comment createComment( CommentRequest commentRequest) {
          UUID  checklistId= commentRequest.getChecklistId();
          Checklist checklist=  checklistService.getChecklistChecklistId(checklistId);
          Task task= taskService.getTaskById(checklist.getTaskId());
          UUID memberId = memberRepository.getMemberId(userId(),task.getBoardId());
          String role= memberRepository.getRolePMByBoardIdAndUserId(task.getBoardId(),userId());
          UUID pmId= memberRepository.getPmId(userId(),task.getBoardId());
          System.out.println("role:" + role);

          if (RoleName.ROLE_MANAGER.name().equals(role)){
              System.out.println("pmId:" + pmId);
              return   commentRepository.createComment(checklistId, commentRequest , LocalDateTime.now(), pmId);
          }
          UUID teamLeadId= memberRepository.getTeamleadUUID(userId(),checklist.getTaskId());

          if (teamLeadId != null) {
              return   commentRepository.createComment(checklistId, commentRequest , LocalDateTime.now(), teamLeadId);
          }
          if (memberId == null) {
              throw new ForbiddenException("You are not member in this board");
          }
          return commentRepository.createComment(checklistId, commentRequest , LocalDateTime.now(), memberId);



    }

    @Override
    public Comment getCommentByCommentId(UUID commentId) {
        Comment comment= commentRepository.getCommentByCommentId(commentId);

        if (comment == null) {
            throw new NotFoundException("Comment not found");
        }
        return comment;
    }

    @Override
    public Set<Comment> getAllComments(UUID checkListId) {
        Set<Comment> comments = new HashSet<>(commentRepository.getAllComments(checkListId));
        if (comments.isEmpty()) {
            throw new NotFoundException("Comment not found");
        }
        return comments;
        }


    @Override
    public Void deleteCommentByCommentId(UUID commentId) {

        Comment  comment= getCommentByCommentId(commentId);
        Checklist checklist= checklistService.getChecklistChecklistId(comment.getChecklistId());
        Task task= taskService.getTaskById(checklist.getTaskId());
        String role= memberRepository.getRoleInTask(task.getBoardId(),userId(), task.getTaskId());
        System.out.println(task.getTaskId());
        System.out.println("role in task: "+ role);
        if (role == null) {
            UUID memberId= memberRepository.getMemberId(userId(),task.getBoardId());
            if (memberId ==null){
                throw new ForbiddenException("Member not found");
            }
            if(memberId.equals(comment.getCommentBy())){
                 commentRepository.deleteCommentByCommentId(commentId);
            }else {
                throw new ForbiddenException("This Comment does not belong to yours");
            }
        }
        if (RoleName.ROLE_MANAGER.name().equals(role) || RoleName.ROLE_LEADER.name().equals(role)){
             commentRepository.deleteCommentByCommentId(commentId);
        }
        return null;
    }


    @Override
    public Comment UpdateCommentByCommentId(UUID commentId, CommentRequest commentRequest) {
        Comment  comment = getCommentByCommentId(commentId);
        System.out.println(comment.getChecklistId());
        Checklist checklist= checklistService.getChecklistChecklistId(comment.getChecklistId());
        System.out.println(checklist);
        Task task= taskService.getTaskById(checklist.getTaskId());
        String role= memberRepository.getRoleInTask(task.getBoardId(),userId(), task.getTaskId());
        System.out.println("role:" + role);
        Comment comment1 = new Comment();
        if (role == null) {
            UUID memberId= memberRepository.getMemberId(userId(),task.getBoardId());
            if (memberId ==null){
                throw new NotFoundException("You are not member in this board");
            }
            if(memberId.equals(comment.getCommentBy())){
                comment1 = commentRepository.UpdateCommentByCommentId(commentId, commentRequest);
            }else {
                throw new ForbiddenException("This Comment does not belong to yours");
            }
        }
        if (RoleName.ROLE_MANAGER.name().equals(role) || RoleName.ROLE_LEADER.name().equals(role)){
           UUID teamLeastId= memberRepository.getTeamleadUUID(userId(),checklist.getTaskId());
           UUID pmId= memberRepository.getPmId(userId(),task.getBoardId());
            System.out.println(userId()+ "board"+task.getBoardId());
            System.out.println("pmId:" + pmId);
           if (teamLeastId != null) {
               if (teamLeastId.equals(comment.getCommentBy())) {
                   comment1 = commentRepository.UpdateCommentByCommentId(commentId, commentRequest);
                   System.out.println("Team lead");
               }else {
                   throw new ForbiddenException("This Comment does not belong to yours team lead");
               }
           } else if (pmId != null) {
               if (pmId.equals(comment.getCommentBy())) {
                   comment1 = commentRepository.UpdateCommentByCommentId(commentId, commentRequest);
                   System.out.println("Pm lead");
               }else {
                   throw new ForbiddenException("This Comment does not belong to yours pm");
               }
           }else {
               throw new ForbiddenException("This Comment does not belong to yours");
           }
        }
        return comment1;
    }
    private boolean canModifyComment(Comment comment, Task task) {
        String role = memberRepository.getRoleInTask(task.getBoardId(), userId(), task.getTaskId());
        if (role == null) {
            UUID memberId = memberRepository.getMemberId(userId(), task.getBoardId());
            return memberId != null && memberId.equals(comment.getCommentBy());
        }
        return role.equals(RoleName.ROLE_MANAGER.name()) || role.equals(RoleName.ROLE_LEADER.name());
    }


}
