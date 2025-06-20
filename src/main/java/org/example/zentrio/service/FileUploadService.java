package org.example.zentrio.service;

import com.google.api.services.drive.model.File;
import org.example.zentrio.dto.response.DriveFileDTO;
import org.example.zentrio.dto.response.Res;
import org.example.zentrio.enums.FileTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface FileUploadService {
    List<DriveFileDTO> getAllFilesByFolderId(String accessToken, String folderId) throws IOException, GeneralSecurityException;

    List<File> getFilesByMimeTypeInFolder(String accessToken, String mimeType, String folderId) throws GeneralSecurityException, IOException;

    File getFileById(String accessToken, String fileId) throws GeneralSecurityException, IOException;


    void deleteFileById(String accessToken, String fileId) throws GeneralSecurityException, IOException;

    File createDriveFile(String accessToken, String name, FileTypes type, String folderId) throws GeneralSecurityException, IOException;

    File renameDriveFile(String accessToken, String fileId, String newName) throws GeneralSecurityException, IOException;

    Res uploadImageToRootDrive(String accessToken, MultipartFile multipartFile) throws GeneralSecurityException, IOException;

    Res uploadFileToFolderDrive(String accessToken, String folderId, MultipartFile multipartFile) throws GeneralSecurityException, IOException;

    ResponseEntity<?> downloadFileDrive(String fileId, String accessToken);
}
