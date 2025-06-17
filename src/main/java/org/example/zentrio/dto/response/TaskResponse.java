package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskResponse {
    private UUID taskId;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private Integer taskOrder;
    private String stage;
    private UUID boardId;
    private UUID ganttBarId;
    private GanttBarResponse ganttBar;
    private UUID createdBy;
    private MemberResponseData creator;
    private MemberResponseData teamLeader;
    private List<ChecklistRespone> allChecklists;
}
