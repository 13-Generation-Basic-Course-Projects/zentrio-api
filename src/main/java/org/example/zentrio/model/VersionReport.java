package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VersionReport {
    private UUID reportVersionId;
    private Map<String, Object> data;
    private UUID boardId;
    private int version;
    private LocalDateTime createdAt;
    private UUID reportId;
}
