package org.example.zentrio.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class MonthlyTaskSummary {
    private String month;
    private int taskCount;
    private int subTaskCount;
    private String completionRate; // <-- changed from double to String
    private int version;
}

