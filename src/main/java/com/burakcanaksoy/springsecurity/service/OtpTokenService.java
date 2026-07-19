package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.OtpToken;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpTokenService {

    private final OtpTokenRepository otpTokenRepository;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public OtpToken createOtpToken(Employee employee) {
        otpTokenRepository.deleteByEmployeeId(employee.getId());

        String otpCode = String.format("%06d", RANDOM.nextInt(999999));

        OtpToken otpToken = OtpToken.builder()
                .employee(employee)
                .otpCode(otpCode)
                .expiryDate(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        return otpTokenRepository.save(otpToken);
    }

    @Transactional
    public void verifyOtp(Employee employee, String otpCode) {
        OtpToken otpToken = otpTokenRepository
                .findValidOtp(employee.getId(), otpCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired OTP code."));

        if (otpToken.getExpiryDate().isBefore(Instant.now())) {
            otpTokenRepository.delete(otpToken);
            throw new ResourceNotFoundException("Invalid or expired OTP code.");
        }

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);
    }


}
