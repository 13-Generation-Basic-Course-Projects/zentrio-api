package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.UserResponse;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.service.impl.AppUserServiceImpl;
import org.example.zentrio.service.impl.AuthServiceImpl;
import org.example.zentrio.service.impl.UserServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/users")
@Tag(name = "Users Controller")
public class UserController {


    private final UserServiceImpl userServiceImpl;

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable("email") String email) {
        ApiResponse<UserResponse> apiResponse = ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Get user by email successfully")
                .status(HttpStatus.OK)
                .payload(userServiceImpl.getUserProfileByEmail(email))
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }
}
