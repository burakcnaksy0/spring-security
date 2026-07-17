package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.config.JwtConfig;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.PasswordResetToken;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtConfig jwtConfig;

    public PasswordResetToken createPasswordResetToken(Employee employee) {
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .employee(employee)
                .expiryDate(Instant.now().plusMillis(jwtConfig.getVerificationExpiration()))
                .build();

        return passwordResetTokenRepository.save(passwordResetToken);
    }

    public void verifyPasswordResetToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Invalid password reset token"));
        if (passwordResetToken.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new RuntimeException("Password reset token has expired. Please send again");
        }
    }

    public void deleteToken(PasswordResetToken passwordResetToken) {
        passwordResetTokenRepository.delete(passwordResetToken);
    }

    public PasswordResetToken getByToken(String token){
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token).orElseThrow(() ->
                new ResourceNotFoundException("Invalid token"));
        return passwordResetToken;
    }
}
