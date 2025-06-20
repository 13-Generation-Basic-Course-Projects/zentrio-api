package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.*;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.dto.response.TokenResponse;
import org.example.zentrio.enums.Verification;
import org.example.zentrio.service.AuthService;
import org.example.zentrio.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("api/v1/auths")
@RequiredArgsConstructor
@Tag(name = "Auth Controller")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a user", description = "Register user with credential")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AppUserResponse>> createUser(@Valid @RequestBody AppUserRequest request) {
            ApiResponse<AppUserResponse> response = ApiResponse.<AppUserResponse>builder()
                    .success(true)
                    .message("Crated successfully")
                    .payload(authService.register(request))
                    .status(HttpStatus.CREATED)
                    .timestamp(LocalDateTime.now())
                    .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login a user", description = "Login user with email verified")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody AuthRequest request) {
        ApiResponse<TokenResponse> response = ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("Logged in successfully")
                .payload(authService.login(request))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Verify otp for user", description = "Verify user with email and OTP")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<?>> verify(@Valid @RequestParam String email, @RequestParam String otp, @RequestParam Verification type) {
        authService.verify(email, otp,type);
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("Verified OTP successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Resend OTP user", description = "Resend OTP for user verification")
    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<?>> resend(@Valid @RequestParam String email,Verification type) {
        authService.resend(email,type);
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .message("Resend OTP successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @Operation(summary = "Login with third party", description = "Login user with third party with credential from third party are provided")
    @PostMapping("/login/third-party")
    public ResponseEntity<ApiResponse<TokenResponse>> loginThirdParty(@RequestBody @Valid AuthThirdPartyRequest request) throws GeneralSecurityException, IOException {
        ApiResponse<TokenResponse> apiResponse = ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("Login successful! Authentication token generated.")
                .status(HttpStatus.CREATED)
                .payload(authService.loginThirdParty(request))
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Operation(summary = "Reset password", description = "Reset password for user that are forgot the password")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordRequest>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,String email) {
        ApiResponse<ResetPasswordRequest> apiResponse = ApiResponse.<ResetPasswordRequest>builder()
                .success(true)
                .message("Reset password successfully.")
                .status(HttpStatus.CREATED)
                .payload(authService.resetPassword(request,email))
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        System.out.println("Refresh token: " + request.getRefreshToken());
        ResponseEntity<?> serviceResponse = refreshTokenService.refreshToken(request);
        if (!serviceResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(serviceResponse.getStatusCode()).body(
                    ApiResponse.builder()
                            .success(false)
                            .message(Objects.requireNonNull(serviceResponse.getBody()).toString())
                            .status((HttpStatus) serviceResponse.getStatusCode())
                            .payload(null)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }
        System.out.println(serviceResponse.getBody());
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .status(HttpStatus.OK)
                        .payload(serviceResponse.getBody())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


}
