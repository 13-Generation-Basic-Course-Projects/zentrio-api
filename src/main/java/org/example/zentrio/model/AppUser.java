package org.example.zentrio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.zentrio.enums.Gender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class AppUser implements UserDetails {
    private UUID userId;
    private String username;
    private String email;
    private String password;
    private Gender gender;
    private String provider;
    private String profileImage;
    private Boolean isVerified;
    private Boolean isReset;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String position;
    private String location;
    private String contact;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
//
//    @Override
//    public String getPassword() {
//        return password;
//    }
//
//    @Override
//    public String getUsername() {
//        return email;
//    }
}
