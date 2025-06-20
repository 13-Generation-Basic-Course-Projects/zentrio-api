package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
}

