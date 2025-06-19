package org.example.zentrio.service.impl;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import lombok.RequiredArgsConstructor;

import org.example.zentrio.dto.response.DriveFileDTO;
import org.example.zentrio.dto.response.Res;
import org.example.zentrio.enums.FileTypes;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.Document;
import org.example.zentrio.repository.DocumentRepository;
import org.example.zentrio.service.FileService;
import org.example.zentrio.service.FileUploadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final DocumentServiceImpl documentService;
    private final DocumentRepository documentRepository;


    public Document validateFolder(String folderId ){
        Document document= documentRepository.getDocumentByFolderId(folderId,documentService.userId());
        if (document==null){
            throw new NotFoundException("Document not found");
        }
        return document;
    }

    private String getFileExtension(String exportMimeType) {
        return switch (exportMimeType) {
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
    private String getExportMimeType(String googleMimeType) {
        return switch (googleMimeType) {
            case "application/vnd.google-apps.document" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; // .docx
            case "application/vnd.google-apps.spreadsheet" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; // .xlsx
            case "application/vnd.google-apps.presentation" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"; // .pptx
            default -> null;
        };
    }




    @Override
    public List<DriveFileDTO> getAllFilesByFolderId(String accessToken, String folderId) throws IOException, GeneralSecurityException {

        Drive drive = documentService.createDriveService(accessToken);
        //validateFolder(folderId);

        List<DriveFileDTO> dtoList = new ArrayList<>();
        String query = String.format("'%s' in parents and trashed = false", folderId);

        FileList result = drive.files().list()
                .setQ(query)
                .setFields("nextPageToken, files(id, name, mimeType, webViewLink, permissions)")
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            throw new NotFoundException("No files found");
        }

        for (File file : files) {
            boolean isPublic = false;
            List<Permission> permissions = file.getPermissions();

            if (permissions != null) {
                for (Permission perm : permissions) {
                    if ("anyone".equals(perm.getType()) && "reader".equals(perm.getRole())) {
                        isPublic = true;
                        break;
                    }
                }
            }
            dtoList.add(new DriveFileDTO(
                    file.getId(),
                    file.getName(),
                    file.getMimeType(),
                    file.getWebViewLink(),
                    isPublic
            ));

        }
        return dtoList;
    }

    @Override
    public void deleteFileById(String accessToken, String fileId) throws GeneralSecurityException, IOException {
        Drive drive = documentService.createDriveService(accessToken);


        File fileMetadata;
        try {
            fileMetadata = drive.files().get(fileId)
                    .setFields("id, name, webViewLink, mimeType")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("File not found with ID: " + fileId);
            }
            throw e; // rethrow other unexpected Google exceptions
        }


        drive.files().delete(fileId).execute();
    }


    @Override
    public File getFileById(String accessToken, String fileId) throws GeneralSecurityException, IOException {
        Drive drive = documentService.createDriveService(accessToken);
        File fileMetadata;
        try {
            fileMetadata = drive.files().get(fileId)
                    .setFields("id, name, webViewLink, mimeType")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("File not found with ID: " + fileId);
            }
            throw e; // rethrow other unexpected Google exceptions
        }


        return fileMetadata;
    }


    @Override
    public File createDriveFile(String accessToken, String name, FileTypes userInputType, String folderId) throws GeneralSecurityException, IOException {
        // Convert user input to official Google MIME type
        String mimeType = getGoogleMimeType(userInputType.toString());
        //validateFolder(folderId);

        Drive drive = documentService.createDriveService(accessToken);
        File fileMetadata;
        try {
            fileMetadata = drive.files().get(folderId)
                    .setFields("id, name, webViewLink, mimeType")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("Folder not found with ID: " + folderId);
            }
            throw e; // rethrow other unexpected Google exceptions
        }


        File file = new File();
        file.setName(name);
        file.setMimeType(mimeType);


        if (folderId != null && !folderId.isEmpty()) {
            file.setParents(Collections.singletonList(folderId));
        }

        File createdFile = drive.files().create(file)
                .setFields("id, name, mimeType, webViewLink")
                .execute();

        return createdFile;
    }


    @Override
    public File renameDriveFile(String accessToken, String fileId, String newName) throws GeneralSecurityException, IOException {

        Drive drive = documentService.createDriveService(accessToken);

        File fileMetadata;
        try {
            fileMetadata = drive.files().get(fileId)
                    .setFields("id, name, webViewLink, mimeType")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("Folder not found with ID: " + fileId);
            }
            throw e; // rethrow other unexpected Google exceptions
        }

        // If it's a folder, don't allow renaming here
        if ("application/vnd.google-apps.folder".equals(fileMetadata.getMimeType())) {
            throw new IllegalArgumentException("Cannot rename a folder using this method.");
        }

        // Proceed to rename the file

        File updatedFile = drive.files().update(fileId, fileMetadata)
                .setFields("id, name, webViewLink, mimeType")
                .execute();

        return updatedFile;
    }


    @Override
    public List<File> getFilesByMimeTypeInFolder(String accessToken, String mimeType, String folderId)
            throws GeneralSecurityException, IOException {
        String type = getGoogleMimeType(mimeType);
        //validateFolder(folderId);
        Drive drive = documentService.createDriveService(accessToken);
        File fileMetadata;
        try {
            fileMetadata = drive.files().get(folderId)
                    .setFields("id, name, webViewLink, mimeType")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("Folder not found with ID: " + folderId);
            }
            throw e; // rethrow other unexpected Google exceptions
        }

        // If it's a folder, don't allow renaming here
        if (!"application/vnd.google-apps.folder".equals(fileMetadata.getMimeType())) {
            throw new IllegalArgumentException("This id not refer to a folder.");
        }

        String query = String.format(
                "'%s' in parents and mimeType = '%s' and trashed = false",
                folderId, type
        );

        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType, webViewLink)")
                .setOrderBy("name")
                .execute();

        List<File> files = result.getFiles();

        if (files == null || files.isEmpty()) {
            throw new NotFoundException("No files of type " + mimeType + " found in folder ID: " + folderId);

        }


        return files;
    }


    public static String getGoogleMimeType(String input) {
        switch (input.toLowerCase()) {
            case "doc":
            case "document":
                return "application/vnd.google-apps.document";
            case "sheet":
            case "spreadsheet":
                return "application/vnd.google-apps.spreadsheet";
            case "slide":
            case "presentation":
                return "application/vnd.google-apps.presentation";
            case "folder":
                return "application/vnd.google-apps.folder";
            default:
                throw new IllegalArgumentException("Unsupported file type: " + input);
        }
    }

    @Override
    public Res uploadImageToRootDrive(String accessToken, MultipartFile multipartFile) throws GeneralSecurityException, IOException {
        Res res = new Res();

        Drive drive = documentService.createDriveService(accessToken);

        // Step 2: Save MultipartFile to a temporary file
        java.io.File tempFile = java.io.File.createTempFile("upload-", multipartFile.getOriginalFilename());

        try {
            multipartFile.transferTo(tempFile);

            File fileMetadata = new File();
            fileMetadata.setName(multipartFile.getOriginalFilename());
            // Not setting .setParents() → file will go to root (My Drive)

            FileContent mediaContent = new FileContent(multipartFile.getContentType(), tempFile);
            File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            String imageUrl = "https://drive.google.com/uc?export=view&id=" + uploadedFile.getId();

            res.setStatus(201);
            res.setMessage("Image successfully uploaded to My Drive.");
            res.setUrl(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(500);
            res.setMessage("Upload failed: " + e.getMessage());
        } finally {
            tempFile.delete(); // Clean up
        }

        return res;
    }



    @Override
    public Res uploadFileToFolderDrive(String accessToken, String folderId, MultipartFile file)
            throws GeneralSecurityException, IOException {
        //validateFolder(folderId);
        // Validate file name
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("Invalid file name.");
        }

        // Validate file extension and map to Google MIME type
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        String googleMimeType = switch (extension) {
            case "doc", "docx" -> "application/vnd.google-apps.document";
            case "xls", "xlsx" -> "application/vnd.google-apps.spreadsheet";
            case "ppt", "pptx" -> "application/vnd.google-apps.presentation";
            default -> throw new BadRequestException("Unsupported file type: " + extension);
        };

        Drive drive = documentService.createDriveService(accessToken);

        try (InputStream inputStream = file.getInputStream()) {

            File fileMetadata = new File();
            fileMetadata.setName(originalFilename);
            fileMetadata.setMimeType(googleMimeType); // Tell Google to convert
            fileMetadata.setParents(Collections.singletonList(folderId));

            // This is the original MIME type of the file (e.g. docx, xlsx, etc.)
            String originalMimeType = file.getContentType();
            InputStreamContent mediaContent = new InputStreamContent(originalMimeType, inputStream);
            mediaContent.setLength(file.getSize());

            File uploadedFile = drive.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            if (uploadedFile == null || uploadedFile.getId() == null) {
                throw new RuntimeException("Upload failed — Google Drive did not return a file ID.");
            }

            String fileUrl = uploadedFile.getWebViewLink();

            Res res = new Res();
            res.setStatus(201);
            res.setMessage("File successfully uploaded and converted.");
            res.setUrl(fileUrl);

            return res;

        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            throw new BadRequestException("Upload failed: " + e.getMessage());
        }
    }

    @Override

    public ResponseEntity<?> downloadFileDrive(String fileId, String accessToken) {
        try {
            Drive drive = documentService.createDriveService(accessToken);

            File fileMetadata = drive.files().get(fileId)
                    .setFields("id, name, mimeType")
                    .setSupportsAllDrives(true)
                    .execute();

            String mimeType = fileMetadata.getMimeType();
            String name = fileMetadata.getName();

            if ("application/vnd.google-apps.folder".equals(mimeType)) {
                // It's a folder → zip it
                ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
                try (ZipOutputStream zipStream = new ZipOutputStream(zipOutput)) {
                    zipFolderContents(drive, fileId, name + "/", zipStream);
                }

                String zipFileName = URLEncoder.encode(name + ".zip", StandardCharsets.UTF_8).replace("+", "%20");

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + zipFileName)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(zipOutput.toByteArray());
            }

            // It's a file → download/export
            return downloadSingleFile(drive, fileId, fileMetadata);

        } catch (GoogleJsonResponseException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Google Drive error: " + e.getDetails().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Download failed: " + e.getMessage());
        }
    }


    private void zipFolderContents(Drive drive, String folderId, String path, ZipOutputStream zipStream) throws IOException {
        String query = "'" + folderId + "' in parents and trashed = false";
        FileList fileList = drive.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType)")
                .setSupportsAllDrives(true)
                .execute();

        for (File file : fileList.getFiles()) {
            String fullPath = path + file.getName();

            if ("application/vnd.google-apps.folder".equals(file.getMimeType())) {
                // Recurse into subfolder
                zipFolderContents(drive, file.getId(), fullPath + "/", zipStream);
            } else {
                ByteArrayOutputStream fileStream = new ByteArrayOutputStream();

                if (file.getMimeType().startsWith("application/vnd.google-apps.")) {
                    String exportMimeType = getExportMimeType(file.getMimeType());
                    if (exportMimeType == null) continue;

                    drive.files().export(file.getId(), exportMimeType)
                            .executeMediaAndDownloadTo(fileStream);

                    fullPath += getFileExtension(exportMimeType);
                } else {
                    drive.files().get(file.getId())
                            .executeMediaAndDownloadTo(fileStream);

                    fullPath += getFileExtension(file.getMimeType());
                }

                zipStream.putNextEntry(new ZipEntry(fullPath));
                zipStream.write(fileStream.toByteArray());
                zipStream.closeEntry();
            }
        }
    }


    private ResponseEntity<?> downloadSingleFile(Drive drive, String fileId, File metadata) throws IOException {
        String mimeType = metadata.getMimeType();
        String fileName = metadata.getName();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if (mimeType.startsWith("application/vnd.google-apps.")) {
            String exportMimeType = getExportMimeType(mimeType);
            String fileExtension = getFileExtension(exportMimeType);

            if (exportMimeType == null) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("Unsupported Google document type: " + mimeType);
            }

            drive.files().export(fileId, exportMimeType)
                    .executeMediaAndDownloadTo(outputStream);

            mimeType = exportMimeType;
            fileName += fileExtension;
        } else {
            drive.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);

            // Add file extension if needed
            String ext = getFileExtension(mimeType);
            if (!fileName.toLowerCase().endsWith(ext)) {
                fileName += ext;
            }
        }

        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType(mimeType))
                .body(outputStream.toByteArray());
    }

}

