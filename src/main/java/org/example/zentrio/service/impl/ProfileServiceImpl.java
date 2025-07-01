package org.example.zentrio.service.impl;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.zentrio.dto.request.ProfileRequest;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.enums.ImageExtension;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.NotFoundException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.FileMetadata;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.repository.ProfileRepository;
import org.example.zentrio.service.AuthService;
import org.example.zentrio.service.ProfileService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ModelMapper modelMapper;
    private final AuthService authService;
    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;
    private final MinioClient minioClient;


    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${app.base-url}")
    private String baseUrl;


    public String generateFileUrl(String fileName) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/api/v1/files/preview-file/" + fileName)
                .toUriString();
    }

    @Override
    public AppUserResponse getProfile() {

        AppUser appUser = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//        AppUser appUser = appUserRepository.getUserByEmail(email);
        System.out.println("appUser" + appUser.getUsername());

        System.out.println("Email: " + appUser);

        return modelMapper.map(appUser, AppUserResponse.class);
    }

    @Override
    public AppUserResponse updateProfile(ProfileRequest profileRequest) {
        UUID appUserId = authService.getCurrentAppUserId();
//        AppUser users = appUserRepository.getUserById(appUserId);
//        if(profileRequest.getProfileImage() != null) {
//            users.setProfileImage(profileRequest.getProfileImage());
//        }

//
//        // error this upload profile
//        AppUser appUser = profileRepository.updateProfile(profileRequest,appUserId);
        AppUser appUser = profileRepository.updateProfile(profileRequest,appUserId);
        return appUser != null ? modelMapper.map(appUser, AppUserResponse.class) : null;
    }

    @Override
    public void deleteProfile() {
        UUID appUserId = authService.getCurrentAppUserId();
        profileRepository.deleteProfile(appUserId);
    }

    @Override
    public AppUserResponse getProfileByUserId(UUID userId) {
        AppUser user = appUserRepository.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("User with id " + userId + " not found");
        }

        AppUser appUser = profileRepository.getProfileByUserId(userId);
        return appUser != null ? modelMapper.map(appUser, AppUserResponse.class) : null;
    }

    @SneakyThrows
    @Override
    public FileMetadata updateProfileCover(MultipartFile file) {
        UUID appUserId = authService.getCurrentAppUserId();

        List<String> imageExtensions = Arrays.stream(ImageExtension.values())
                .map(ImageExtension::getExtension)
                .toList();

        String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!imageExtensions.contains(fileExtension)) {
            throw new BadRequestException("Profile image must be .png, .svg, .jpg, .jpeg, or .gif");
        }

        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        String fileName = UUID.randomUUID() + "." + fileExtension;

        // Save to DB (associate the file with current user)
        profileRepository.updateProfileCover(appUserId, fileName);

        // Upload to MinIO
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .contentType(file.getContentType())
                .stream(file.getInputStream(), file.getSize(), -1)
                .build()
        );

        // Generate access URL
        String fileUrl = generateFileUrl(fileName);

        return FileMetadata.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }

    @SneakyThrows
    @Override
    public InputStream getPreviewCoverOfUser(String fileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        }catch (Exception e){
            throw new NotFoundException("File note found");
        }
    }


}
