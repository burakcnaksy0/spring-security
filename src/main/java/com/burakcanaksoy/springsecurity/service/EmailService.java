package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.PasswordResetToken;
import com.burakcanaksoy.springsecurity.entity.VerificationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendVerificationEmail(Employee employee, VerificationToken verificationToken) {
        String subject = "Email Verification";
        String verificationUrl = "http://localhost:9094/api/v1/auth/verify?token=" + verificationToken.getToken();
        String message = "Dear " + employee.getUsername() + ",\n\n"
                + "Please click the link below to verify your email:\n"
                + verificationUrl + "\n"
                + "This link will expire in 24 hours.";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(employee.getEmail());
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }

    public void sendPasswordReset(Employee employee, PasswordResetToken passwordResetToken) {
        String subject = "Password Reset";
        String passwordResetUrl = "http://localhost:9094/api/v1/auth/reset-password?token=" + passwordResetToken.getToken();
        String message = "Dear " + employee.getUsername() + ",\n\n"
                + "Please click the link below to reset your password:\n"
                + passwordResetUrl + "\n"
                + "This link will expire in 24 hours.";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(employee.getEmail());
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }
}