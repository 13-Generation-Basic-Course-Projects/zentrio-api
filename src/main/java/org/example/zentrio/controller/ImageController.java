package org.example.zentrio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.model.Announcement;
import org.example.zentrio.model.FileMetadata;
import org.example.zentrio.service.AnnouncementService;
import org.example.zentrio.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Images Controller")
@SecurityRequirement(name = "bearerAuth")
public class ImageController {
    private final FileService fileService;
    private final AnnouncementService announcementService;
    private final SimpMessagingTemplate  messagingTemplate;

    @Operation(summary = "Upload a file", description = "Uploads a file and returns metadata about the uploaded file.")
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadata>> uploadFile(@RequestParam MultipartFile file) {
        System.out.println("Uploading file " + file.getOriginalFilename());
        FileMetadata fileMetadata = fileService.uploadFile(file);
        ApiResponse<FileMetadata> apiResponse = ApiResponse.<FileMetadata>builder()
                .success(true)
                .message("Upload file successfully.")
                .payload(fileMetadata)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @Operation(summary = "Preview a file", description = "Fetches a file by its name and returns the file as a byte array (usually for image previews).")
    @GetMapping("preview-file/{file-name}")
    public ResponseEntity<?> getFileByFileName(@PathVariable("file-name") String fileName) throws IOException {
        InputStream inputStream = fileService.getFileByFileName(fileName);
        byte[] fileContent = inputStream.readAllBytes();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_PNG)
                .body(fileContent);
    }

    @PostMapping(value = "/boards/{board-id}/announcements/{announcement-id}/upload-multiple-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadMultipleFilesForAnnouncement(
            @PathVariable("board-id") UUID boardId,
            @PathVariable("announcement-id") UUID announcementId,
            @RequestParam("files") List<MultipartFile> files) {

        //  Upload files and get metadata list
        List<FileMetadata> metadataList = fileService.uploadFiles(boardId, files ,announcementId);

        // Optionally load the updated announcement and broadcast
        Announcement updated = announcementService.getAnnouncementById(announcementId);
        messagingTemplate.convertAndSend(
                "/topic/board/" + boardId + "/announcements",
                Map.of("type", "UPDATED", "announcement", updated)
        );

        // 4. Return updated announcement in response
        ApiResponse<List<FileMetadata> > apiResponse = ApiResponse.<List<FileMetadata> >builder()
                .success(true)
                .message("Uploaded " + metadataList.size() + " file(s) and updated announcement successfully.")
                .payload(metadataList)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }


}
