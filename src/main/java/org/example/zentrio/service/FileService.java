package org.example.zentrio.service;

import org.example.zentrio.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileService {
    FileMetadata uploadFile(MultipartFile file);

    InputStream getFileByFileName(String fileName);
    String extractFileNameFromUrl(String url);
    void deleteFileByName(String fileName);
    List<FileMetadata> uploadFiles(UUID boardId,List<MultipartFile> files, UUID announcementId);
}
