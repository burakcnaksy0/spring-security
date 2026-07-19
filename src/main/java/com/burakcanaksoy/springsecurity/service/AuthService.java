package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.*;
import com.burakcanaksoy.springsecurity.dto.response.AuthResponse;
import com.burakcanaksoy.springsecurity.dto.response.LoginResponse;
import com.burakcanaksoy.springsecurity.dto.response.OtpResponse;
import com.burakcanaksoy.springsecurity.dto.response.RefreshTokenResponse;
import com.burakcanaksoy.springsecurity.entity.*;
import com.burakcanaksoy.springsecurity.exception.AlreadyExistsException;
import com.burakcanaksoy.springsecurity.exception.EmailNotVerifiedException;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.mapper.EmployeeMapper;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.security.CustomUserPrincipal;
import com.burakcanaksoy.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final OtpTokenService otpTokenService;
    //private final AuthenticationManager authenticationManager;

    public AuthResponse register(EmployeeRegisterRequest registerRequest) {
        checkIfUsernameExists(registerRequest.getUsername());
        checkIfEmailExists(registerRequest.getEmail());
        checkIfPhoneExists(registerRequest.getPhoneNumber());
        checkIfTcExists(registerRequest.getTcNo());
        Employee employee = mapper.toEmployee(registerRequest);
        Employee saved = repository.save(employee);

        VerificationToken verificationToken = verificationTokenService.createVerificationToken(saved);
        emailService.sendVerificationEmail(saved, verificationToken);
        return mapper.toAuthResponse(saved);
    }

    // maile gelen linke tıklayınca arkada bu işlemler gerçekleşiyor;
    public String verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenService.verifyToken(token);
        Employee employee = verificationToken.getEmployee();
        employee.setEnabled(true);
        repository.save(employee);
        verificationTokenService.deleteToken(verificationToken);
        return "Email successfully verified";
    }

    public LoginResponse login(EmployeeLoginRequest loginRequest) {
        Employee employee = repository.findByUsername(loginRequest.getUsername()).orElseThrow(() -> new ResourceNotFoundException("Employee not found with this username : " + loginRequest.getUsername()));

        if (!employee.isEnabled()) {
            throw new EmailNotVerifiedException("Email not verified. Please check your email.");
        }

        matchPassword(loginRequest.getPassword(), employee.getPasswordHash());

        String accessToken = jwtUtil.generateToken(employee);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(employee.getUsername())
                .message("Login successfully")
                .build();
    }


    /*
    public LoginResponse login(EmployeeLoginRequest loginRequest) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        CustomUserPrincipal principal = (CustomUserPrincipal) authenticate.getPrincipal();
        String token = jwtUtil.generateToken(principal);
        return LoginResponse.builder()
                .token(token)
                .username(principal.getUsername())
                .message("Login successfully")
                .build();

    }
     */

    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyRefreshToken)
                .map(RefreshToken::getEmployee)
                .map(employee -> {
                    String accessToken = jwtUtil.generateToken(employee);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(employee.getUsername());
                    return RefreshTokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken.toString())
                            .message("Refresh successful")
                            .build();
                }).orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    public String logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        refreshTokenService.deleteEmployeeId(principal.getId());
        return "Logout successfully with username : " + principal.getUsername();
    }

    public String forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        Employee employee = repository.findByEmail(forgotPasswordRequest.getEmail()).orElseThrow(() ->
                new ResourceNotFoundException("If this email address is registered, a password reset email has been sent."));

        PasswordResetToken passwordResetToken = passwordResetTokenService.createPasswordResetToken(employee);
        emailService.sendPasswordReset(employee, passwordResetToken);
        return "A password reset request was sent to this email address : " + forgotPasswordRequest.getEmail();

    }

    public String resetPassword(String token, ResetPasswordRequest resetPasswordRequest) {
        PasswordResetToken passwordResetToken = passwordResetTokenService.getByToken(token);
        passwordResetTokenService.verifyPasswordResetToken(passwordResetToken.getToken());
        Employee employee = passwordResetToken.getEmployee();
        employee.setPasswordHash(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        repository.save(employee);
        passwordResetTokenService.deleteToken(passwordResetToken);
        return "Your password has been reset. Your new password is : " + resetPasswordRequest.getNewPassword();
    }

    private void checkIfEmailExists(String email) {
        if (repository.existsByEmail(email)) {
            throw new AlreadyExistsException("Email already exists.");
        }
    }

    private void checkIfUsernameExists(String username) {
        if (repository.existsByUsername(username)) {
            throw new AlreadyExistsException("Username already exists.");
        }
    }

    private void checkIfPhoneExists(String phone) {
        if (repository.existsByPhoneNumber(phone)) {
            throw new AlreadyExistsException("Phone number already exists.");
        }
    }

    private void checkIfTcExists(String tcno) {
        if (repository.existsByTcNo(tcno)) {
            throw new AlreadyExistsException("TC number already exists.");
        }
    }

    private void matchPassword(String loginPassword, String dbPasswordHashed) {
        if (!passwordEncoder.matches(loginPassword, dbPasswordHashed)) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    public OtpResponse sendOtp(EmailOtpRequest request) {
        Employee employee = repository.findByUsername(request.getUsername()).orElseThrow(() ->
                new ResourceNotFoundException("Employee not found with this username : " + request.getUsername()));
        if (!employee.isEnabled()) {
            throw new EmailNotVerifiedException("Email not verified.");
        }
        matchPassword(request.getPassword(), employee.getPasswordHash());
        OtpToken otpToken = otpTokenService.createOtpToken(employee);
        emailService.sendOtpEmail(employee, otpToken.getOtpCode());
        return OtpResponse.builder()
                .username(request.getUsername())
                .message("A code has been sent to the email address : " + employee.getEmail())
                .build();

    }

    public LoginResponse verifyOtp(VerifyOtpRequest request) {
        Employee employee = repository.findByUsername(request.getUsername()).orElseThrow(() ->
                new ResourceNotFoundException("Employee not found with this username : " + request.getUsername()));
        otpTokenService.verifyOtp(employee, request.getOtpCode());
        String accessToken = jwtUtil.generateToken(employee);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(request.getUsername())
                .message("Login success")
                .build();
    }
}

/*
Analyze this project thoroughly and ensure that when I run 'docker-compose up -d', the entire system starts up completely.
The docker-compose configuration should handle starting all necessary services including frontend, backend, database, and any other required components.
Review the current docker-compose.yml file and the overall project structure to ensure all services are properly defined and can run together seamlessly.
 The system should be fully operational after the docker-compose command executes.
 */