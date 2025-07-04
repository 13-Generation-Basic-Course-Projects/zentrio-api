package org.example.zentrio.service.impl;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.zentrio.enums.ImageExtension;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ForbiddenException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.FileMetadata;
import org.example.zentrio.repository.AnnouncementImageRepository;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.repository.MemberRepository;
import org.example.zentrio.service.AppUserService;
import org.example.zentrio.service.BoardService;
import org.example.zentrio.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final AnnouncementImageRepository announcementImageRepository;
    private final MemberRepository memberRepository;
    private final BoardService boardService;

    public UUID userId (){
        AppUser appUser= (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();
    }
    public UUID getPmId( UUID boardId ) {
        UUID pmId= memberRepository.getPmId(userId(),boardId);
        if ( pmId == null ) {
            throw new ForbiddenException("Only Pm allow to create announcement");
        }
        return pmId;
    }

    @Value("${minio.bucket.name}")
    private String bucketName;


    @SneakyThrows
    @Override
    public FileMetadata uploadFile(MultipartFile file) {
        System.out.println("file: " + file.getOriginalFilename());
        List<String> imageExtensions = new ArrayList<>();

        for (ImageExtension imageExtension : ImageExtension.values()) {
            imageExtensions.add(imageExtension.getExtension());
        }

        if (!imageExtensions.contains(StringUtils.getFilenameExtension(file.getOriginalFilename()))) {
            throw new BadRequestException("Profile image must be a valid image URL ending with .png, .svg, .jpg, .jpeg, or .gif");
        }

        boolean bucketExits = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

        if (!bucketExits) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        String fileName = file.getOriginalFilename();
        fileName = UUID.randomUUID() + "." + StringUtils.getFilenameExtension(fileName);

      minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .contentType(file.getContentType())
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .build()
        );
        AppUser appUser= appUserRepository.getUserById(userId());
        if (appUser == null) {
            throw new BadRequestException("User not found");
        }
        String profileImage= extractFileNameFromUrl(appUser.getProfileImage());
        deleteFileByName(profileImage);

        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/preview-file/" + fileName)
                .toUriString();

        appUserRepository.profileImage(fileUrl, userId());
        return FileMetadata.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

    }

    @SneakyThrows
    @Override
    public InputStream getFileByFileName(String fileName) {
        try {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );}catch (Exception e){

            throw new NotFoundException("File note found");
        }
    }

    @SneakyThrows
    @Override
    public List<FileMetadata> uploadFiles(UUID boardId, List<MultipartFile> files, UUID announcementId) {
        List<FileMetadata> fileMetadataList = new ArrayList<>();
        boardService.getBoardByBoardId(boardId);
        UUID pmId = getPmId(boardId);
        // Get allowed extensions
        List<String> allowedExtensions = Arrays.stream(ImageExtension.values())
                .map(ImageExtension::getExtension)
                .map(String::toLowerCase)
                .toList();

        // Ensure bucket exists
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(originalName);

            if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
                throw new BadRequestException("Only image files (.png, .jpg, .jpeg, .svg, .gif) are allowed");
            }

            String uniqueFileName = UUID.randomUUID() + "." + extension;

            // Upload to MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(uniqueFileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // Generate accessible URL (if you expose a preview endpoint)
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/files/preview-file/")
                    .path(uniqueFileName)
                    .toUriString();

            FileMetadata metadata = FileMetadata.builder()
                    .fileName(uniqueFileName)
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            fileMetadataList.add(metadata);
            announcementImageRepository.createAnnouncementImage(fileUrl, LocalDateTime.now(),pmId, announcementId);
        }

        return fileMetadataList;
    }

    @Override
    public String extractFileNameFromUrl(String url) {
        if (url == null || !url.contains("/")) {
            throw new BadRequestException("Invalid file URL.");
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @SneakyThrows
    @Override
    public void deleteFileByName(String fileName) {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!bucketExists) {
            throw new BadRequestException("Bucket does not exist.");
        }

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
            System.out.println("Deleted file: " + fileName);
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete file: " + fileName);
        }
    }


}
