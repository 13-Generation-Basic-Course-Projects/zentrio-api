package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentAnnouncementResponse {

    private UUID id;
    private String content;
    private LocalDateTime creationDate;
    private UUID announcementId;
    private UUID commentBy;
    private MemberResponseData member;
}
