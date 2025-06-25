package org.example.zentrio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AttachmentRequest {

    @NotNull
//    @Size(min = 1, max = 100)
    private Map<String, String> details;
}
