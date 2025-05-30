package org.example.zentrio.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.Status;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatusRequest {
    private Status status;
}
