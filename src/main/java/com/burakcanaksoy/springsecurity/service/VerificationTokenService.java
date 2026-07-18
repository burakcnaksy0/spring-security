package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.config.JwtConfig;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.VerificationToken;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {
    private final VerificationTokenRepository verificationTokenRepository;
    private final JwtConfig jwtConfig;

    public VerificationToken createVerificationToken(Employee employee) {
        VerificationToken verificationToken = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .employee(employee)
                .expiryDate(Instant.now().plusMillis(jwtConfig.getVerificationExpiration()))
                .build();
        return verificationTokenRepository.save(verificationToken);
    }

    public VerificationToken verifyToken(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow(() ->
                new ResourceNotFoundException("Invalid verify token"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.delete(verificationToken);
            throw new RuntimeException("Verification token has expired. Please register again.");
        }
        return verificationToken;
    }

    public void deleteToken(VerificationToken verificationToken){
        verificationTokenRepository.delete(verificationToken);
    }
}
