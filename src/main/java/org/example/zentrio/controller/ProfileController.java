package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.ProfileRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.model.FileMetadata;
import org.example.zentrio.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/profiles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile Controller")
public class ProfileController {
    private final ProfileService profileService;

    @Operation(summary = "Get user profile", description = "Fetches the details of the currently authenticated user.")
    @GetMapping("/preview-profile")
    public ResponseEntity<ApiResponse<AppUserResponse>> getProfile() {
        ApiResponse<AppUserResponse> apiResponse = ApiResponse.<AppUserResponse>builder()
                .success(true)
                .message("Get profile successfully")
                .payload(profileService.getProfile())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Update user profile", description = "Updates the details of the currently authenticated user. Provide the necessary fields in the request body.")
    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateProfile(@Valid @RequestBody ProfileRequest profileRequest) {
        ApiResponse<AppUserResponse> apiResponse = ApiResponse.<AppUserResponse>builder()
                .success(true)
                .message("Updated profile successfully")
                .payload(profileService.updateProfile(profileRequest))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Delete user profile", description = "Deleted the currently authenticated user's profile. This action cannot be undone.")
    @DeleteMapping("/delete-profile")
    public ResponseEntity<ApiResponse<?>> deleteProfile() {
        ApiResponse<AppUserResponse> apiResponse = ApiResponse.<AppUserResponse>builder()
                .success(true)
                .message("Deleted profile successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get user profile by user ID",description = "Get user profile by user ID")
    @GetMapping("/users/{user-id}/profiles")
    public ResponseEntity<ApiResponse<AppUserResponse>> getProfileByUserId(@PathVariable("user-id") UUID userId) {
        ApiResponse<AppUserResponse> apiResponse = ApiResponse.<AppUserResponse>builder()
                .success(true)
                .message("Get user profile by ID successfully")
                .status(HttpStatus.OK)
                .payload(profileService.getProfileByUserId(userId))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Update only current users profile",description = "Update only current users profile")
    @PutMapping(value = "/users/profile-image",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadata>> updateCoverOfUserProfile(
            @RequestParam("file") MultipartFile file) {
        FileMetadata fileMetadata = profileService.updateProfileCover(file);
        ApiResponse<FileMetadata> apiResponse = ApiResponse.<FileMetadata>builder()
                .success(true)
                .message("Profile cover uploaded successfully")
                .payload(fileMetadata)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    @Operation(summary = "Get cover of users",description = "Get cover of current user back")
    @GetMapping("/preview-image/{file-name}")
    public ResponseEntity<?> getPreviewCoverOfUser(@PathVariable("file-name") String fileName) throws IOException {
        InputStream inputStream = profileService.getPreviewCoverOfUser(fileName);
        byte[] fileContent = inputStream.readAllBytes();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_PNG)
                .body(fileContent);
    }

}
