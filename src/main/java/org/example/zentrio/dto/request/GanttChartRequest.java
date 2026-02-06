package org.example.zentrio.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.model.Board;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GanttChartRequest {


    @NotNull
    @NotBlank
    @Size(min = 2, max = 100)
    private String title;

    @NotNull
    private UUID boardId;

}
