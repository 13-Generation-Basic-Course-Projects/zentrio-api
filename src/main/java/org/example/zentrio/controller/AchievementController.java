package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.AchievementRequest;
import org.example.zentrio.dto.response.AchievementResponse;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Achievement;
import org.example.zentrio.service.AchievementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/achievements")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Achievement Controller")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @Operation(summary = "Get achievement", description = "Get achievement for a user")
    @GetMapping("/current")
    ResponseEntity<ApiResponse<AchievementResponse>> getAllAchievementsByCurrentUser() {
        ApiResponse<AchievementResponse> apiResponse = ApiResponse.<AchievementResponse>builder()
                .success(true)
                .message("Get achievement successfully")
                .payload(achievementService.getAllAchievementByCurrentUser())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Set up achievement", description = "set up achievement for a user")
    @PostMapping
    public ResponseEntity<ApiResponse<Achievement>> createAchievement(@Valid @RequestBody AchievementRequest achievementRequest) {
        ApiResponse<Achievement> apiResponse = ApiResponse.<Achievement>builder()
                .success(true)
                .message("Create achievement successfully")
                .payload(achievementService.createAchievement(achievementRequest))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Operation(summary = "Update achievement", description = "Updated achievement for a user")
    @PutMapping
    public ResponseEntity<ApiResponse<Achievement>> updateAchievement(@Valid @RequestBody AchievementRequest achievementRequest) {
        System.out.println("Details: " + achievementRequest.getDetails());
        ApiResponse<Achievement> apiResponse = ApiResponse.<Achievement>builder()
                .success(true)
                .message("Updated achievement successfully")
                .payload(achievementService.updateAchievement(achievementRequest))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Deleted achievement for current users",description = "Deleted achievement for current users")
    @DeleteMapping("/achievement-id")
    public ResponseEntity<?> deleteAchievement(@RequestParam("achievement-id") UUID achievementId) {
        achievementService.deleteAchievement(achievementId);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Deleted all achievement successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get achievement by user ID",description = "Get achievement by user ID when visited")
    @GetMapping("/users/{user-id}")
    public ResponseEntity<?> getAchievementByUserId(@PathVariable("user-id") UUID userId) {
        ApiResponse<Map<String,Object>> apiResponse = ApiResponse.< Map<String,Object> >builder()
                .success(true)
                .message("Get achievement by user ID successfully")
                .payload(achievementService.getAchievementByUserId(userId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Get achievement for current user only achievement",description = "Get only achievement of user")
    @GetMapping("/my-achievement")
    public ResponseEntity<?> getMyAchievement() {
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Get achievement successfully")
                .payload(achievementService.getMyAchievement())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }



}
