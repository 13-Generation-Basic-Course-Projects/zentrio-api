package org.example.zentrio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.Gender;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileRequest {

    @NotNull
    @NotBlank
    @Size(min = 2, max = 50)
    private String username;

    @NotNull
    private Gender gender;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 100)
    private String position;

    @NotNull
    @NotBlank
    private String location;

    @NotNull
    @NotBlank
    private String contact;

  //  private String profileImage;

}
