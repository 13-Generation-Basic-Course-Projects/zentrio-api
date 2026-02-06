package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.CommentAnnouncementRequest;
import org.example.zentrio.dto.response.CommentAnnouncementResponse;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.Announcement;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Comment;
import org.example.zentrio.repository.AnnouncementRepository;
import org.example.zentrio.repository.CommentRepository;
import org.example.zentrio.repository.MemberRepository;
import org.example.zentrio.service.AnnouncementService;
import org.example.zentrio.service.CommentAnnouncementService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentAnnouncementServiceImplement implements CommentAnnouncementService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementService announcementService;

    public UUID userId(){
        AppUser appUser= (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();
    }

    @Override
    public CommentAnnouncementResponse createAnnouncementComment(CommentAnnouncementRequest commentAnnouncementRequest) {
        Announcement announcement = announcementService.getAnnouncementById(commentAnnouncementRequest.getAnnouncementId());
        UUID pmId= memberRepository.getPmId(userId(),announcement.getBoardId());

        CommentAnnouncementResponse commentAnnouncementResponse = null;
        if (pmId != null){
            System.out.println("pm"+ pmId);
          return   commentAnnouncementResponse=   commentRepository.createCommentAnnouncement(announcement.getAnnouncementId(),commentAnnouncementRequest, LocalDateTime.now(), pmId);
        }
        UUID memberId= memberRepository.getOneMemberId(userId(),announcement.getBoardId());

        if (memberId != null){
            System.out.println("member"+ memberId);
            commentAnnouncementResponse=    commentRepository.createCommentAnnouncement(announcement.getAnnouncementId(),commentAnnouncementRequest, LocalDateTime.now(), memberId);
        }else{
            throw new ForbiddenException("you don't have permission to create comment");
        }

        return commentAnnouncementResponse;
    }

    @Override
    public CommentAnnouncementResponse getCommentByCommentId(UUID commentId) {
        CommentAnnouncementResponse comment= commentRepository.getCommentAnnouncementByCommentId(commentId);

        if (comment == null) {
            throw new NotFoundException("Comment not found");
        }
        return comment;
    }

    @Override
    public Set<CommentAnnouncementResponse> getAllAnnouncementComments(UUID announcementId) {
        announcementService.getAnnouncementById(announcementId);
        Set<CommentAnnouncementResponse> comments = new HashSet<>(commentRepository.getAllAnnouncementComments(announcementId));
        if (comments.isEmpty()) {
            throw new NotFoundException("Comment not found");
        }
        return comments;
    }

    @Override
    public void deleteAnnouncementCommentByCommentId(UUID commentId) {

        CommentAnnouncementResponse comment =  getCommentByCommentId(commentId);
        Announcement announcement=   announcementService.getAnnouncementById(comment.getAnnouncementId());
        UUID pmId= memberRepository.getPmId(userId(),announcement.getBoardId());
        boolean your = false;
        if (pmId != null){
            commentRepository.deleteAnnouncementCommentByCommentId(commentId,announcement.getAnnouncementId());
            your=true;
        }
        List<UUID> memberId= memberRepository.getAllMemberId(userId(), announcement.getBoardId());
        if (memberId == null){
            throw new ForbiddenException("you don't have permission to delete comment");
        }
        for (UUID memberId1 : memberId){
            if (memberId1.equals(comment.getCommentBy())){
                commentRepository.deleteAnnouncementCommentByCommentId(commentId,announcement.getAnnouncementId());
                your=true;
            }
        }
        if (!your){
            throw new ForbiddenException("you don't have permission to delete comment");
        }

    }

    @Override
    public CommentAnnouncementResponse UpdateAnnouncementCommentByCommentId(UUID commentId, CommentAnnouncementRequest commentAnnouncementRequest) {
     CommentAnnouncementResponse comment=  getCommentByCommentId(commentId);
     Announcement announcement= announcementService.getAnnouncementById(commentAnnouncementRequest.getAnnouncementId());
        boolean your = false;
        CommentAnnouncementResponse commentAnnouncementResponse = null;
        List<UUID> memberId= memberRepository.getAllMemberId(userId(), announcement.getBoardId());
        if (memberId == null){
            throw new ForbiddenException("you don't have permission to delete comment");
        }
        for (UUID memberId1 : memberId){
            if (memberId1.equals(comment.getCommentBy())){
                commentAnnouncementResponse=    commentRepository.UpdateAnnouncementCommentByCommentId(commentId, commentAnnouncementRequest,memberId1);
               your=true;
            }

        }
        if (!your){
            throw new ForbiddenException("you don't have permission to delete comment");
        }
        return commentAnnouncementResponse;
    }


}
