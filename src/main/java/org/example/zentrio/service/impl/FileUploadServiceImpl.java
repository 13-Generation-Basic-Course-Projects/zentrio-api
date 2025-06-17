package org.example.zentrio.service.impl;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;

import org.example.zentrio.dto.response.Res;
import org.example.zentrio.enums.FileTypes;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.Document;
import org.example.zentrio.repository.DocumentRepository;
import org.example.zentrio.service.FileService;
import org.example.zentrio.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

    @Override
    public List<File> getAllFilesByFolderId(String accessToken, String folderId) throws IOException, GeneralSecurityException {

        Drive drive = documentService.createDriveService(accessToken);
        validateFolder(folderId);

        String query= String.format("'%s' in parents and trashed = false", folderId);

        FileList result =drive.files().list()
                .setQ(query)
                .setFields("nextPageToken, files(id, name, mimeType, webViewLink)")
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            throw new NotFoundException("No files found");
        }
        return files;
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
        validateFolder(folderId);

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
        validateFolder(folderId);
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
        validateFolder(folderId);
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





}

