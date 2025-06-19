package org.example.zentrio.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Select;
import org.example.zentrio.service.OtpService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 3;
    private static final String OTP_PREFIX = "otp:";
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String generateOtp(String email) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            int digit = random.nextInt(10);
            otp.append(digit);
        }
        String otpKey = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(otpKey, otp.toString(),OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        return otp.toString();
    }

    @Override
    public Boolean validateOtp(String email,String inputOtp) {
        String otpKey = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if(storedOtp != null && storedOtp.equals(inputOtp)){
            redisTemplate.delete(otpKey);
            return true;
        }
        return false;
    }
}
