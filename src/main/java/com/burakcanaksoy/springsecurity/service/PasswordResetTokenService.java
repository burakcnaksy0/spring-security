package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.PasswordResetToken;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public String createPasswordResetToken(Employee employee) {
        passwordResetTokenRepository.deleteByEmployeeId(employee.getId());

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .hashedToken(hashedToken)
                .employee(employee)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        passwordResetTokenRepository.save(passwordResetToken);
        return rawToken;
    }


    public void deleteToken(PasswordResetToken passwordResetToken) {
        passwordResetTokenRepository.delete(passwordResetToken);
    }


    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public PasswordResetToken getByRawToken(String rawToken) {
        String hashedToken = hashToken(rawToken);
        return passwordResetTokenRepository.findByHashedToken(hashedToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid password reset token"));
    }

    /*
    public void verifyPasswordResetToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Invalid password reset token"));
        if (passwordResetToken.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new RuntimeException("Password reset token has expired. Please send again");
        }
    }
    public PasswordResetToken getByToken(String token) {
        return passwordResetTokenRepository.findByToken(token).orElseThrow(() ->
                new ResourceNotFoundException("Invalid token"));
    }
     */
}
