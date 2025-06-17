package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.dto.response.StageTaskGroup;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Report {

    private UUID reportId;
    private LocalDate creationDate;
    private Integer version;
    private  UUID boardId;
    private Board boardData;
    private List<AllMember> allMembers;
    private String boardName;
//    private List<TaskRespone> allTasks;
    private List<StageTaskGroup> groupedTasks;
}
