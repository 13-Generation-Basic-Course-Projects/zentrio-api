package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.dto.response.GanttBarResponse;
import org.example.zentrio.dto.response.MemberResponse;
import org.example.zentrio.dto.response.MemberResponseData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
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
    private List<Feedback> feedbacks;


}