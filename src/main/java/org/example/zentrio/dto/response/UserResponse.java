package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.Gender;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserResponse {
    private UUID userId;
    private String username;
    private Gender gender;
    private String email;
    private String profileImage;
    private Boolean isVerified;
    private Boolean isReset;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String position;
    private String location;
    private String contact;
}
