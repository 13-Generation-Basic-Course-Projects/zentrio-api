package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;
import org.example.zentrio.model.AllTasks;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StageTaskGroup {
    private String stage;
    private int count;
    private List<TaskResponse> tasks;

}
