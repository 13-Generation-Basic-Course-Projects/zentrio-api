package org.example.zentrio.service;


import org.example.zentrio.dto.request.ProfileRequest;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface ProfileService {
    AppUserResponse getProfile();

    AppUserResponse updateProfile(ProfileRequest profileRequest);

    void deleteProfile();

    AppUserResponse getProfileByUserId(UUID userId);

    FileMetadata updateProfileCover(MultipartFile file);

    InputStream getPreviewCoverOfUser(String fileName);
}
