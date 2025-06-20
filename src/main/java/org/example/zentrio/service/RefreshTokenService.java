package org.example.zentrio.service;

import org.example.zentrio.dto.request.RefreshTokenRequest;
import org.example.zentrio.model.RefreshToken;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(RefreshToken refreshToken);

    Optional<RefreshToken> validateRefreshToken(String token);

    void deleteTokensByUser(UUID userId);

    ResponseEntity<?> refreshToken(RefreshTokenRequest request);
}
