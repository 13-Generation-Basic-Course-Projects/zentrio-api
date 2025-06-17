package org.example.zentrio.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.DocumentRequest;
import org.example.zentrio.enums.FileTypes;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.Board;
import org.example.zentrio.model.Document;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.repository.BoardRepository;
import org.example.zentrio.repository.DocumentRepository;
import org.example.zentrio.service.DocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final BoardRepository boardRepository;
    private final AppUserRepository appUserRepository;



    public UUID userId() {
        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();
    }

    public Drive createDriveService(String accessToken) throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        HttpRequestInitializer requestInitializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
        };

        Drive drive = new Drive.Builder(httpTransport, jsonFactory, requestInitializer)
                .setApplicationName("User Drive Uploader")
                .build();


        // 🔎 Validate the token by making a simple request
        try {
            drive.files().list()
                    .setPageSize(1)
                    .setFields("files(id)")  // Lightweight call
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                throw new BadRequestException("❌ Access token is expired or invalid.");
            } else {
                throw e;
            }
        }
        return drive;
    }


    @Override
    public List<Document> getAllDocuments(UUID boardId) {
//
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board not found");
        }

        return documentRepository.getAllFolders(boardId, userId());
    }

    @Override
    public List<Document> getAllPublicDocument(UUID boardId) {
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board not found");
        }
        List<Document> documents = documentRepository.getAllPublicDocument(boardId);
        if (documents.isEmpty()) {
            throw new NotFoundException("No Public Document Found");
        }
        return documents;
    }

    @Override
    public List<Document> getDocumentByType(UUID boardId, FileTypes mimeType) {
        List<Document> document = getAllDocuments(boardId);
        if (document.isEmpty()) {
            throw new NotFoundException("Document not found");
        }
        String mimeTypeString = mimeType.toString();
        String type = getGoogleMimeType(mimeTypeString);
        List<Document> documentList = documentRepository.getDocumentByType(userId(), boardId, type);
        if (documentList.isEmpty()) {
            throw new NotFoundException("Document not found");
        }
        return documentList;

    }

    @Override
    public Document getDocumentById(UUID documentId) {

        System.out.println("kk");
        Document document = documentRepository.getDocumentById(documentId, userId());
        if (document == null) {

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
    public void shareFolder(UUID documentId, String emailAddress, String accessToken) throws IOException, GeneralSecurityException {
        Document document = getDocumentById(documentId);
        Drive drive = createDriveService(accessToken);
        String   folderId =  document.getFolderId();
        File folder = drive.files().get(folderId)
                .setFields("id, name, webViewLink, iconLink")
                .setSupportsAllDrives(true)
                .execute();

        // Create permission object
        Permission permission = new Permission()
                .setType("user") // Share with a user
                .setRole("reader") // User will have "reader" access
                .setEmailAddress(emailAddress);

        // Apply the permission to the folder
        drive.permissions().create(folderId, permission).setSendNotificationEmail(true).execute();

        System.out.println("Folder shared successfully with: " + emailAddress);
    }


    @Override
    public String publicDocument(UUID documentId, String accessToken) throws GeneralSecurityException, IOException {

        Document document = getDocumentById(documentId);
        if (document == null) {
            throw new NotFoundException("Document not found");
        }
        String folderId = document.getFolderId();
        Drive drive = createDriveService(accessToken);

        File folder = drive.files().get(folderId)
                .setFields("id, name, webViewLink, iconLink")
                .setSupportsAllDrives(true)
                .execute();


        Permission permission = new Permission()
                .setType("anyone")
                .setRole("reader");

        drive.permissions().create(folderId, permission)
                .setSendNotificationEmail(false)
                .setSupportsAllDrives(true)
                .execute();
        documentRepository.publicFolder(userId(), documentId, true);
        return "Folder shared successfully with: " + folder.getWebViewLink();
    }

    @Override
    public List<Document> getDocumentByName(String documentName, UUID boardId) {

        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Document not found");
        }
        List<Document> documents = documentRepository.getDocumentByName(boardId, documentName, userId());
        if (documents.isEmpty()) {
            throw new NotFoundException("Document not found");
        }
        return documents;
    }

    @Override
    public Document createFolder(DocumentRequest documentRequest) throws GeneralSecurityException, IOException {

        Board board = boardRepository.getBoardByBoardId(documentRequest.getBoardId());
        if (board == null) {
            throw new NotFoundException("Board not found with ID: " + documentRequest.getBoardId());
        }
        String documentType = getGoogleMimeType(documentRequest.getTypes().toString());
        Drive drive = createDriveService(documentRequest.getAccessToken());

        // Prepare folder metadata
        File fileMetadata = new File();
        fileMetadata.setName(documentRequest.getFolderName());
        fileMetadata.setMimeType(documentType);

        // Create the folder

        File folder = drive.files().create(fileMetadata)
                .setFields("id, name,mimeType, webViewLink")  // Only requesting the 'id' field from the response
                .execute();
        if (folder == null) {
            throw new NotFoundException("No folder created");
        }
        Document document = documentRepository.createFolder(LocalDateTime.now(), folder.getMimeType(), userId(), documentRequest.getBoardId(), folder.getWebViewLink(), folder.getName(), folder.getId());


        return document;

    }

    @Override
    public Document updateDocumentName(String accessToken, UUID documentId, String newFolderName) throws GeneralSecurityException, IOException {
        Drive drive = createDriveService(accessToken);

        Document document = getDocumentById(documentId);
        if (document == null) {
            throw new NotFoundException("Document not found with ID: " + documentId);
        }
        String folderId = document.getFolderId();

        // Prepare the updated metadata
        File fileMetadata = new File();
        fileMetadata.setName(newFolderName);
        // Optional: check that the file is actually a folder before updating

        // Update the folder's metadata
        File updatedFolder;

        try {
            updatedFolder = drive.files().update(folderId, fileMetadata)
                    .setFields("id, name, webViewLink")  // Specify the fields to be returned (name in this case)
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("Folder not found with ID: " + folderId);
            }
            throw e; // rethrow other unexpected Google exceptions
        }
        File existingFile = drive.files().get(folderId).setFields("mimeType").execute();

        Document updateDocument = documentRepository.updateFolderName(userId(), document.getBoardId(), newFolderName, documentId);

        return updateDocument;

    }

    @Override
    public void deleteDocumentById(String accessToken, UUID documentId)
            throws GeneralSecurityException, IOException {
        Document document = getDocumentById(documentId);
        if (document == null) {
            throw new NotFoundException("Document not found with ID: " + documentId);
        }
        String folderId = document.getFolderId();

        Drive drive = createDriveService(accessToken);

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


        drive.files().delete(folderId).execute();
        documentRepository.deleteDocumentById(documentId, userId());

    }


    @Override
    public Document uploadDocumentToDrive(
            String accessToken,
            MultipartFile multipartFile,
            UUID boardId
    ) throws GeneralSecurityException, IOException {

        // Validate board
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board not found with ID: " + boardId);
        }

        // Detect file type and validate
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("Invalid file name.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        String googleMimeType = switch (extension) {
            case "doc", "docx" -> "application/vnd.google-apps.document";
            case "xls", "xlsx" -> "application/vnd.google-apps.spreadsheet";
            case "ppt", "pptx" -> "application/vnd.google-apps.presentation";
            default -> throw new BadRequestException("Unsupported file type: " + extension);
        };

        Drive drive = createDriveService(accessToken);

        // Save to temp file
        java.io.File tempFile = java.io.File.createTempFile("upload-", originalFilename);
        multipartFile.transferTo(tempFile);

        try {
            File fileMetadata = new File();
            fileMetadata.setName(originalFilename);
            fileMetadata.setMimeType(googleMimeType);

            FileContent mediaContent = new FileContent(multipartFile.getContentType(), tempFile);

            File uploadedFile = drive.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, name, mimeType, webViewLink")
                    .execute();

            if (uploadedFile == null) {
                throw new RuntimeException("Upload failed — Google Drive did not return a file.");
            }

            return documentRepository.createFolder(
                    LocalDateTime.now(),
                    uploadedFile.getMimeType(),
                    userId(),
                    boardId,
                    uploadedFile.getWebViewLink(),
                    uploadedFile.getName(),
                    uploadedFile.getId()
            );

        } finally {
            tempFile.delete();
        }
    }

    @Override
    public String privateDocument(UUID documentId, String accessToken) throws GeneralSecurityException, IOException {
        Document document = getDocumentById(documentId);
        if (document == null) {
            throw new NotFoundException("Document not found");
        }
        String folderId = document.getFolderId();
        Drive drive = createDriveService(accessToken);

        // Fetch existing permissions on the folder
        PermissionList permissions = drive.permissions()
                .list(folderId)
                .setSupportsAllDrives(true)
                .execute();

        // Remove "anyone" permission if exists
        for (Permission permission : permissions.getPermissions()) {
            if ("anyone".equals(permission.getType())) {
                drive.permissions().delete(folderId, permission.getId())
                        .setSupportsAllDrives(true)
                        .execute();
                break; // Assuming only one 'anyone' permission
            }
        }

//        documentRepository.publicFolder(userId(), documentId, false); // Set document to private (false)

        File folder = drive.files().get(folderId)
                .setFields("id, name, webViewLink, iconLink")
                .setSupportsAllDrives(true)
                .execute();
        documentRepository.publicFolder(userId(), documentId, false);

        return "Folder access restricted successfully: " + folder.getWebViewLink();
    }

    @Override
    public ResponseEntity<?> downloadDocument(UUID documentId, String accessToken) {


        Document document = documentRepository.getDocumentByDocumentId(documentId);
        if (document == null) {
            throw new NotFoundException("Document not found with ID: " + documentId);
        }

        String fileId = document.getFolderId(); // or getFileId() if that's more accurate
        if ("application/vnd.google-apps.folder".equals(document.getDocumentType())) {
            throw new NotFoundException("The provided ID refer to a folder.");
        }
        try {
            Drive drive = createDriveService(accessToken);

            File fileMetadata = drive.files().get(fileId)
                    .setFields("name, mimeType")
                    .execute();

            String mimeType = fileMetadata.getMimeType();
            String fileName = fileMetadata.getName();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

            if (mimeType.startsWith("application/vnd.google-apps.")) {
                // It's a Google-native file (Docs, Sheets, Slides)
                String exportMimeType =    getExportMimeType(mimeType);
                String fileExtension = getFileExtension(exportMimeType);

                if (exportMimeType == null) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body("Unsupported Google document type: " + mimeType);
                }

                drive.files().export(fileId, exportMimeType)
                        .executeMediaAndDownloadTo(outputStream);

                mimeType = exportMimeType;
                fileName += fileExtension; // Add correct extension like .docx
            } else {
                // Regular binary file, download as-is
                drive.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);
            }

            byte[] fileBytes = outputStream.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(fileBytes);

        } catch (GoogleJsonResponseException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Google Drive error: " + e.getDetails().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Download failed: " + e.getMessage());
        }
    }


    @Override
    public ResponseEntity<?> downloadFolderAsZip(UUID documentId, String accessToken) {
        Document document = documentRepository.getDocumentByDocumentId(documentId);
        if (document == null) {
            throw new NotFoundException("Document not found with ID: " + documentId);
        }

        String folderId = document.getFolderId(); // or getFileId() if that's more accurate

        if (!"application/vnd.google-apps.folder".equals(document.getDocumentType())) {
            throw new NotFoundException("The provided ID does not refer to a folder.");
        }

        try {
            Drive drive = createDriveService(accessToken);

            // Step 1: List all files inside the folder
            FileList fileList = drive.files().list()
                    .setQ("'" + folderId + "' in parents and trashed = false")
                    .setFields("files(id, name, mimeType)")
                    .execute();

            List<File> files = fileList.getFiles();

//


            ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(zipOutputStream);

            for (File file : files) {
                String fileId = file.getId();
                String mimeType = file.getMimeType();
                String fileName = file.getName();
                ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();

                if (mimeType.startsWith("application/vnd.google-apps.")) {
                    String exportMimeType = getExportMimeType(mimeType);
                    String fileExtension = getFileExtension(exportMimeType);

                    if (exportMimeType != null) {
                        drive.files().export(fileId, exportMimeType)
                                .executeMediaAndDownloadTo(fileOutput);
                        fileName += fileExtension;
                    } else {
                        continue; // skip unsupported Google-native type
                    }
                } else {
                    // Binary files
                    drive.files().get(fileId)
                            .executeMediaAndDownloadTo(fileOutput);
                }

                // Write to ZIP
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(fileOutput.toByteArray());
                zos.closeEntry();
            }

            zos.close();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"folder.zip\"")
                    .body(zipOutputStream.toByteArray());

        } catch (GoogleJsonResponseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Google API error: " + e.getDetails().getMessage());
        } catch (IOException | GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Download failed: " + e.getMessage());
        }
    }

    @Override
    public Void saveGoogleEmailDrive(String email) {
        AppUser appUser= appUserRepository.getUserById(userId());
        if ( appUser.getGoogleEmail()== null ) {

         return  appUserRepository.saveGoogleEmailDrive(email, userId());
        }else  {

            if (!appUser.getGoogleEmail().equals(email)) {
                throw new BadRequestException("The provided email address not right");
            }

            return null;
        }
    }

    @Override
    public List<Document> getAllDocumentsAllUser(UUID boardId) {
        Board board = boardRepository.getBoardByBoardId(boardId);
        if (board == null) {
            throw new NotFoundException("Board not found");
        }
        List<Document> documentList= documentRepository.getAllDocumentsAllUser(boardId);
        if (documentList.isEmpty()){
           throw new NotFoundException("No documents found");
        }
        return documentList;
    }


}

