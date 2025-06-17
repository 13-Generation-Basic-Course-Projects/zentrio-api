package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.ChecklistRequest;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.enums.ChecklistStatus;
import org.example.zentrio.enums.Status;
import org.example.zentrio.model.Checklist;
import org.example.zentrio.model.FileMetadata;
import org.example.zentrio.service.ChecklistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Checklist Controller")
public class ChecklistController {

    private final ChecklistService checklistService;

    @Operation(summary = "Create checklist by tasks ID", description = "Created checklist with specific tasks ID")
    @PostMapping
    public ResponseEntity<ApiResponse<Checklist>> createChecklist(
            @Valid @RequestBody ChecklistRequest checklistRequest) {
        ApiResponse<Checklist> apiResponse = ApiResponse.<Checklist>builder()
                .success(true)
                .message("Created checklist by task ID successfully")
                .payload(checklistService.createChecklist(checklistRequest))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }


    @Operation(summary = "Get checklist by ID", description = "Get checklist by ID with current tasks")
    @GetMapping("/{checklist-id}")
    public ResponseEntity<ApiResponse<Checklist>> getChecklistById(@PathVariable("checklist-id") UUID checklistId) {
        ApiResponse<Checklist> apiResponse = ApiResponse.<Checklist>builder()
                .success(true)
                .message("Get checklist by ID successfully")
                .payload(checklistService.getChecklistChecklistId(checklistId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Get all checklist by tasks ID", description = "Get all checklist by specific tasks ID")
    @GetMapping("/tasks/{task-id}")
    public ResponseEntity<ApiResponse<HashSet<Checklist>>> getAllChecklistsByTaskId(
            @PathVariable("task-id") UUID taskId) {
        ApiResponse<HashSet<Checklist>> apiResponse = ApiResponse.<HashSet<Checklist>>builder()
                .success(true)
                .message("Get checklist by ID successfully")
                .payload(checklistService.getAllChecklistsByTaskId(taskId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Update checklist by ID and task ID ", description = "Updated checklist by ID with specific tasks ID")
    @PutMapping("/{checklist-id}")
    public ResponseEntity<ApiResponse<Checklist>> updateChecklistByIdAndTaskId(
            @Valid @RequestBody ChecklistRequest checklistRequest,
            @PathVariable("checklist-id") UUID checklistId) {
        ApiResponse<Checklist> apiResponse = ApiResponse.<Checklist>builder()
                .success(true)
                .message("Updated checklist successfully")
                .payload(checklistService.updateChecklistByIdAndTaskId(checklistRequest, checklistId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Delete checklist by ID and task ID", description = "Deleted checklist by ID with specific tasks ID")
    @DeleteMapping("/{checklist-id}/tasks/{task-id}")
    public ResponseEntity<ApiResponse<Checklist>> deleteChecklistByIdAndTaskId(
            @PathVariable("checklist-id") UUID checklistId,
            @PathVariable("task-id") UUID taskId) {
        ApiResponse<Checklist> apiResponse = ApiResponse.<Checklist>builder()
                .success(true)
                .message("Deleted checklist successfully")
                .payload(checklistService.deleteChecklistByIdAndTaskId(checklistId, taskId))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Upload cover of checklist by checklist ID",description = "Upload cover of checklist specific checklist ID")
    @PostMapping(value = "/{checklistId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadata>> uploadCover(
            @PathVariable UUID checklistId,
            @RequestParam("file") MultipartFile file){
        FileMetadata fileMetadata = checklistService.uploadChecklistCoverImage(checklistId,file);
        ApiResponse<FileMetadata> apiResponse = ApiResponse.<FileMetadata>builder()
                .success(true)
                .message("Checklist cover uploaded successfully")
                .payload(fileMetadata)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);
    }

    @Operation(summary = "Get cover of checklist by checklist ID and File Name")
    @GetMapping("/{checklist-id}/cover/{file-name}")
    public ResponseEntity<?> checklistCoverByFileName(@PathVariable("checklist-id") UUID checklistId, @PathVariable("file-name") String fileName) throws IOException {
        InputStream inputStream = checklistService.getFileByFileName(checklistId,fileName);
        byte[] fileContent = inputStream.readAllBytes();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_PNG)
                .body(fileContent);
    }


    @Operation(summary = "Update status of checklist by ID",description = "Update status of checklist by specific checklist ID")
    @PutMapping("/{checklist-id}/status")
    public ResponseEntity<?> updateStatusOfChecklistById(
            @PathVariable("checklist-id") UUID checklistId,
            ChecklistStatus status){
        checklistService.updateStatusOfChecklistById(checklistId,status);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Updated status of checklist successfully")
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Assigned member to checklist", description = "Assigned member to checklist with specific members ID")
    @PostMapping("{checklist-id}/tasks/{task-id}/members/{assigned-by}/members/{assigned-to}/")
    public ResponseEntity<?> assignMemberToChecklist(
            @PathVariable("checklist-id") UUID checklistId,
            @PathVariable("task-id") UUID taskId,
            @PathVariable("assigned-by") UUID assignedBy,
            @PathVariable("assigned-to") UUID assignedTo) {
        checklistService.assignMemberToChecklist(checklistId, taskId, assignedBy, assignedTo);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Assigned member to checklist successfully")
                .payload(null)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }



    @Operation(summary = "Get all checklists",description = "Get all checklist")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<HashSet<Checklist>>> getAllChecklists() {
        ApiResponse<HashSet<Checklist>> apiResponse = ApiResponse.<HashSet<Checklist>>builder()
                .success(true)
                .message("Get all checklists successfully")
                .payload(checklistService.getAllChecklists())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }


    @Operation(summary = "Get all checklist by current users",description = "Get all checklists by current users")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<HashSet<Checklist>>> getAllChecklistsByCurrentUser() {
        ApiResponse<HashSet<Checklist>> apiResponse = ApiResponse.<HashSet<Checklist>>builder()
                .success(true)
                .message("Get all checklists for current users successfully")
                .payload(checklistService.getAllChecklistsByCurrentUser())
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Move order checklist in task")
    @PutMapping("/orders/tasks/{task-id}")
    public ResponseEntity<ApiResponse<Void>> moveChecklistOrder(
            @PathVariable("task-id") UUID taskId ,
            @RequestParam int newOrder,
            @RequestParam int oldOrder) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(true)
                .message("Move checklist in task successfully")
                .payload(checklistService.moveChecklistOrder(taskId,newOrder,oldOrder))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

}