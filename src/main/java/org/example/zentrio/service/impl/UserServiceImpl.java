package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.UserResponse;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.service.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserResponse getUserProfileByEmail(String email) {
        UserResponse userResponse = appUserRepository.getUserProfileByEmail(email);
        if(userResponse == null) {
            throw new NotFoundException("User not found");
        }
        return appUserRepository.getUserProfileByEmail(email);
    }
}
