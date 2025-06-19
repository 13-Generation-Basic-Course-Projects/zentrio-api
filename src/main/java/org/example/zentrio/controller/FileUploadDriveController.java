package org.example.zentrio.controller;


import com.google.api.services.drive.model.File;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.example.zentrio.dto.response.ApiResponse;
import org.example.zentrio.dto.response.DriveFileDTO;
import org.example.zentrio.dto.response.Res;
import org.example.zentrio.enums.FileTypes;
import org.example.zentrio.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Drive Upload Controller")
public class FileUploadDriveController {

    private final FileUploadService fileService;


    @GetMapping("/getFileBy-FolderID")
    @Operation(summary= "Get all files by folder id")
    public ResponseEntity<ApiResponse<List<DriveFileDTO>>> getAllFilesByFolderId(@RequestParam String accessToken,
                                                                                 @RequestParam String folderId) throws IOException, GeneralSecurityException {

        ApiResponse<List<DriveFileDTO>> response =  ApiResponse.<List<DriveFileDTO>>builder()
                .success(true)
                .message("file get successfully ")
                .payload(fileService.getAllFilesByFolderId(accessToken,folderId))
                .status(HttpStatus.FOUND)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getFileBy-typ")
    @Operation(summary= "Get all files by file type")
    public ResponseEntity<ApiResponse<List<File>>> getFilesByMimeTypeInFolder(@RequestParam String accessToken,
                                                 @RequestParam  String mimeType ,
                                                 @RequestParam  String folderId ) throws IOException, GeneralSecurityException {

        ApiResponse<List<File>> response =  ApiResponse.<List<File>>builder()
                .success(true)
                .message("file get successfully ")
                .payload(fileService.getFilesByMimeTypeInFolder(accessToken,mimeType, folderId))
                .status(HttpStatus.FOUND)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
        // Call the DriveService to get the folders
     //   return fileSevice.getFilesByMimeTypeInFolder(accessToken,mimeType, folderId);
    }

    @GetMapping("/file")
    @Operation(summary= "Get  files by file id")
    public ResponseEntity<ApiResponse<File>> getFileById(@RequestParam String accessToken,
                                         @RequestParam String fileId) throws GeneralSecurityException, IOException {

        ApiResponse<File> response = ApiResponse.<File>builder()
                .success(true)
                .message("file get successfully ")
                .payload(fileService.getFileById(accessToken, fileId))
                .status(HttpStatus.FOUND)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/file")
    @Operation(summary= "Delete fie by file id")
    public ResponseEntity<ApiResponse<String>> deleteFileById(@RequestParam String accessToken,
                                                 @RequestParam String fileId) throws GeneralSecurityException, IOException {
        fileService.deleteFileById(accessToken, fileId);
        ApiResponse<String> response =  ApiResponse.<String>builder()
                .success(true)
                .message("file create successfully ")
                .status(HttpStatus.ACCEPTED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }



    @PostMapping("/create/{file-type}")
    @Operation(summary= "Create drive file")
    public ResponseEntity<ApiResponse<File>> createDriveFile(
            @RequestParam("accessToken") String accessToken,
            @RequestParam("name") String name,
            @PathVariable("file-type") FileTypes type, // e.g., doc, sheet, slide
            @RequestParam(value = "folderId", required = false) String folderId) throws GeneralSecurityException, IOException {
        ApiResponse<File> response =  ApiResponse.<File>builder()
                .success(true)
                .message("file create successfully ")
                .status(HttpStatus.ACCEPTED)
                .payload(fileService.createDriveFile(accessToken, name, type, folderId))
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(201).body(response);


//        try {
//            String fileId = fileSevice.createDriveFile(accessToken, name, type, folderId);
//            return ResponseEntity.ok("File created successfully. File ID: " + fileId);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body("Invalid file type: " + e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating file: " + e.getMessage());
//        }
    }


    @PatchMapping("/rename")
    @Operation(summary= "Rename file ")
    public ResponseEntity<ApiResponse<File>> renameFile(
            @RequestParam("accessToken") String accessToken,
            @RequestParam("fileId") String fileId,
            @RequestParam("newName") String newName) throws GeneralSecurityException, IOException {
        ApiResponse<File> response =  ApiResponse.<File>builder()
                .success(true)
                .message("rename file  successfully ")
                .payload(fileService.renameDriveFile(accessToken, fileId, newName))
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
//
//        try {
//            String renamed = fileSevice.renameDriveFile(accessToken, fileId, newName);
//            return ResponseEntity.ok("File renamed successfully to: " + renamed);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Failed to rename file: " + e.getMessage());
//        }
    }


    @PostMapping(value = "/upload-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary= "Upload all file to drive road")
    public ResponseEntity<ApiResponse<Res>> uploadWithUserToken(
            @RequestParam("token") String accessToken,
            @RequestParam("file") MultipartFile multipartFile
    ) throws IOException, GeneralSecurityException {

        ApiResponse<Res> response =  ApiResponse.<Res>builder()
                .success(true)
                .message("file create successfully ")
                .payload(fileService.uploadImageToRootDrive(accessToken, multipartFile))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);

    }

    @GetMapping(value = "/{file-id}/download")
    @Operation(summary= "Download  file from drive ")
    public ResponseEntity<?> downloadFileDrive(@PathVariable("file-id") String fileId, @RequestParam String accessToken) {
        return fileService.downloadFileDrive(fileId, accessToken);
    }


    @PostMapping(value = "/upload-image-to-drive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary= "Upload image to folder")
    public ResponseEntity<ApiResponse<Res>>  uploadImageToFolderDrive (
            @RequestParam("token") String accessToken,
            @RequestParam("folderId") String folderId,
            @RequestParam("file") MultipartFile multipartFile
    ) throws IOException, GeneralSecurityException {

        ApiResponse<Res> response =  ApiResponse.<Res>builder()
                .success(true)
                .message("file create successfully ")
                .payload(fileService.uploadFileToFolderDrive(accessToken, folderId, multipartFile))
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);


    }







}
