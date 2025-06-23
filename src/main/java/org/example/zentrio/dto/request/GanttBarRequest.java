package org.example.zentrio.dto.request;


import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.Face;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GanttBarRequest {
    @NotNull
    @NotBlank
    @Size(min = 2, max = 100)
    private String title;

    @NotNull
    @FutureOrPresent
    private LocalDateTime startedAt;

    @NotNull
    @FutureOrPresent
    private LocalDateTime finishedAt;

    @NotNull
    private Face face;

    @NotNull
    private UUID ganttChartId;



}
