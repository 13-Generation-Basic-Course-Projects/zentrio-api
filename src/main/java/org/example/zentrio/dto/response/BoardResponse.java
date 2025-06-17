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
public class BoardResponse {
    private UUID boardId;
    private String title;
    private String description;
    private String cover;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isFavourite;
    private UUID workspaceId;
    private List<TaskResponse> allTasks;
}
