package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.RefreshTokenRequest;
import org.example.zentrio.jwt.JwtService;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.model.RefreshToken;
import org.example.zentrio.repository.RefreshTokenRepository;
import org.example.zentrio.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    public RefreshToken createRefreshToken(RefreshToken refreshToken) {
        UUID userId = ((AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.insertToken(token);
        return token;
    }


    @Override
    public Optional<RefreshToken> validateRefreshToken(String token) {
        RefreshToken found = refreshTokenRepository.findByToken(token);
        if (found == null || found.getExpiryDate().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(found);
    }

    @Override
    public void deleteTokensByUser(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    public ResponseEntity<?> refreshToken(RefreshTokenRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token is required");
        }
        Optional<RefreshToken> tokenOpt = validateRefreshToken(request.getRefreshToken());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }
        UUID userId = tokenOpt.get().getUserId();
        String newAccessToken = jwtService.generateAccessToken(userId);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}
