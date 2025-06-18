package org.example.zentrio.service;

import org.example.zentrio.dto.response.UserResponse;

public interface UserService {
    UserResponse getUserProfileByEmail(String email);
}
