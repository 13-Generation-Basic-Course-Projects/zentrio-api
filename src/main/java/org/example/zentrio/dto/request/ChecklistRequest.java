package org.example.zentrio.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChecklistRequest {
    @NotBlank(message = "Title cannot be blank!")
    private String title;
//    private Integer checklistOrder;
    @JsonSerialize
    @JsonFormat
    @FutureOrPresent
    private LocalDateTime startedAt;
    @JsonSerialize
    @JsonFormat
    @FutureOrPresent
    private LocalDateTime finishedAt;
//    private UUID taskId;
}