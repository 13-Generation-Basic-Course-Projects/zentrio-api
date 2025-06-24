package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.model.AllTasks;
import org.example.zentrio.model.Attachment;
import org.example.zentrio.model.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChecklistRespone {
    private UUID checklistId;
    private String title;
    private String description;
    private String status;
    private String cover;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer checklistOrder;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private UUID taskId;
    private UUID createdBy;
    private List<MemberResponseData> members;
    private Attachment attachment;
    private List<Comment> allComments;


    public boolean isCompleted() {
        return status.equals("COMPLETED");
    }
}
