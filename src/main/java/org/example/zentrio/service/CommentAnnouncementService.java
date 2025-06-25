package org.example.zentrio.service;

import jakarta.validation.Valid;
import org.example.zentrio.dto.request.CommentAnnouncementRequest;
import org.example.zentrio.dto.response.CommentAnnouncementResponse;

import java.util.Set;
import java.util.UUID;

public interface CommentAnnouncementService {
    CommentAnnouncementResponse createAnnouncementComment( CommentAnnouncementRequest commentAnnouncementRequest);

    CommentAnnouncementResponse getCommentByCommentId(UUID commentId);

    Set<CommentAnnouncementResponse> getAllAnnouncementComments(UUID announcementId);

    void deleteAnnouncementCommentByCommentId(UUID commentId);

    CommentAnnouncementResponse UpdateAnnouncementCommentByCommentId(UUID commentId, CommentAnnouncementRequest commentAnnouncementRequest);
}
