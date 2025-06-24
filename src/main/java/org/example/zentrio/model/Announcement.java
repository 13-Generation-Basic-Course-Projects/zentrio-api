package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.dto.response.CommentAnnouncementResponse;
import org.example.zentrio.dto.response.MemberResponseData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement  {
    private UUID announcementId;
    private String title;
    private String description;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID authorId;
    private MemberResponseData createdBy;
    private List<String>  imageUrl;
    private UUID boardId;
    private List<React> reacts;
    private List<CommentAnnouncementResponse>  comments;
}
