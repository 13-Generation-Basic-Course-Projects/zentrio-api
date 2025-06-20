package org.example.zentrio.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.Gender;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppUserRequest {
    @NotBlank
    @NotNull
    @Size(min = 2, max = 50)
    private String username;

    @NotNull
    private Gender gender;

    @NotNull
    @NotBlank
    @Email
    private String email;

    @NotNull
    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String password;

    private String profileImage;
}
