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
public class AllGanttBarsReponse {
    private UUID ganttBarId;
    private String title;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private UUID ganttChartId;
    private String face;
    private List<MemberResponseData> teamLeader;
    private List<TaskResponse> allTasks;
}
