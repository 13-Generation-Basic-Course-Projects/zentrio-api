package org.example.zentrio.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.example.zentrio.dto.request.*;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.dto.response.TokenResponse;
import org.example.zentrio.enums.Gender;
import org.example.zentrio.enums.Verification;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.exception.ConflictException;
import org.example.zentrio.jwt.JwtService;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.repository.AuthRepository;
import org.example.zentrio.service.AuthService;
import org.example.zentrio.service.EmailService;
import org.example.zentrio.service.OtpService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Override
    public AppUserResponse register(AppUserRequest request) {
        AppUser appUserByEmail = appUserRepository.getUserByEmail(request.getEmail());
        if (appUserByEmail != null) {
            throw new ConflictException("Email already in use try again");
        }

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new BadRequestException("Email is required");
        }

        String profileImage = request.getProfileImage();
        if (profileImage == null || profileImage.isEmpty() || profileImage.equalsIgnoreCase("string")) {
            if (request.getGender().equals(Gender.FEMALE)) {
                request.setProfileImage("https://i.pinimg.com/736x/60/a4/04/60a4046baaa616fd41ee84cf3ccc7953.jpg");
            } else if (request.getGender().equals(Gender.MALE)) {
                request.setProfileImage("https://i.pinimg.com/736x/3e/9f/08/3e9f085ce52735854f9f2d4742f86659.jpg");
            } else {
                request.setGender(Gender.RATHER_NOT_TO_SAY);
                request.setProfileImage("https://i.pinimg.com/736x/d0/7b/a6/d07ba6dcf05fa86c0a61855bc722cb7a.jpg");
            }
        }




        request.setPassword(passwordEncoder.encode(request.getPassword()));
        AppUser appUser = authRepository.register(request);
        String otp = otpService.generateOtp(appUser.getEmail());
        Map<String, Object> variables = new HashMap<>();
        variables.put("otp", otp);

        emailService.sendDynamicEmail(
                appUser.getEmail(),
                "index", // You can rename this to "verification-otp" if needed
                "Verify Your Email - OTP Code",
                variables
        );

        emailService.sendEmail(otp, appUser.getEmail());
        return modelMapper.map(appUser, AppUserResponse.class);
    }

    @Override
    public TokenResponse login(AuthRequest request) {
        authenticate(request.getEmail(), request.getPassword());
        AppUser appUser = appUserRepository.getUserByEmail(request.getEmail());
        if (!appUser.getIsVerified()) {
            throw new BadRequestException("Your email isn't verified to use try again");
        }
        String token = jwtService.generateToken(request.getEmail());
        return TokenResponse.builder()
                .token(token)
                .build();
    }

    @Override
    public void verify(String email, String otp, Verification type) {
        AppUser appUser = appUserRepository.getUserByEmail(email);
        if (appUser == null) {
            throw new BadRequestException("This email does not exist");
        }

        boolean isValidateOtp = otpService.validateOtp(email, otp);
        if (!isValidateOtp) {
            throw new BadRequestException("The OTP entered is invalid or has expired. Please request for getting a new OTP and try again.");
        }

        switch (type) {
            case VERIFY: {
                if (appUser.getIsVerified()) {
                    throw new BadRequestException("This email is already verified");
                }
                appUserRepository.updateVerify(appUser.getUserId());
                break;
            }
            case RESET: {
                appUserRepository.updatedIsResetToTrue(appUser.getUserId());
                break;
            }
            default : throw new BadRequestException("Invalid OTP");
        }


    }

    @Override
    public void resend(String email, Verification type) {
        AppUser appUser = appUserRepository.getUserByEmail(email);

        if (appUser == null) {
            throw new BadRequestException("This email isn't matched");
        }

        switch (type) {
            case VERIFY: {
                if (appUser.getIsVerified()) {
                    throw new BadRequestException("This is email already verified");
                }
                String otp = otpService.generateOtp(appUser.getEmail());
                emailService.sendEmail(otp, email);
                break;
            }
            case RESET: {
                String otp = otpService.generateOtp(appUser.getEmail());
                emailService.sendEmail(otp, email);
                break;
            }
            default:
                throw new BadRequestException("Invalid OTP");
        }

    }

    @Override
    public UUID getCurrentAppUserId() {
        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUser.getUserId();
    }

    public void authenticate(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException e) {
            throw new BadRequestException("Invalid email or password. Please try again later");
        }
    }


    @Override
    public AppUserResponse registerThirdParty(ThirdPartyRequest request) {
        String password = passwordEncoder.encode(UUID.randomUUID().toString());
        AppUser appUser = authRepository.registerThirdParty(request, password);
        return modelMapper.map(appUser, AppUserResponse.class);
    }

    @Override
    public TokenResponse loginThirdParty(AuthThirdPartyRequest request) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
//                .setAudience(Collections.singletonList("67453975102-v5k4olq9qom0dop0400ika07ntf9lils.apps.googleusercontent.com")) own google id ("Fanau")
                .setAudience(Collections.singleton("555065390048-dcgfpec66hk6e3oqs46nhuqlpg4a8c3f.apps.googleusercontent.com"))
                .build();

        GoogleIdToken idToken = verifier.verify(request.getIdToken());

        if (idToken == null) {
            throw new BadRequestException("Invalid ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        if(appUserRepository.getUserByEmail(email) != null) {
            throw new BadRequestException("This email is already logged in.");
        }
//        String googleId = (passwordEncoder.encode(payload.getSubject()));
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        Boolean emailVerified = (Boolean) payload.get("email_verified");
        authRepository.insertUser(email, name, googleId, picture, emailVerified);
        String token = jwtService.generateToken(email);
        return TokenResponse.builder()
                .token(token)
                .build();
    }


    @Override
    public ResetPasswordRequest resetPassword(ResetPasswordRequest request, String email) {
        AppUser appUser = appUserRepository.getUserByEmail(email);
        if (appUser == null) {
            throw new BadRequestException("This email does not matched");
        }

        if (!appUser.getIsReset()) {
            throw new BadRequestException("This email isn't verify for reset");
        }

        String password = (request.getPassword());
        String confirmPassword = (request.getConfirmPassword());

        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }

        request.setPassword(passwordEncoder.encode(password));
        request.setConfirmPassword(passwordEncoder.encode(confirmPassword));
        ResetPasswordRequest resetPasswordRequest = authRepository.resetPassword(request, email);

        appUserRepository.updatedIsResetToFalse(appUser.getUserId());
        System.out.println("is REset : " + appUser.getIsReset());
        return resetPasswordRequest;
    }


}
