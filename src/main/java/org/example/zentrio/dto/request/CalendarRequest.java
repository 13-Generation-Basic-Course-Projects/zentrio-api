package org.example.zentrio.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarRequest {

    @NotNull
    @NotBlank
    private String noted;


    private LocalDateTime startDate;

//    @FutureOrPresent
    private LocalDateTime tillDate;

    @NotNull
    private UUID taskId;

}
