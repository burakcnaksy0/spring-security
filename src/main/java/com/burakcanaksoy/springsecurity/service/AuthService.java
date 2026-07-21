package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.*;
import com.burakcanaksoy.springsecurity.dto.response.*;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final TotpService totpService;
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
        Employee employee = getEmployeeByUsername(loginRequest.getUsername());
        if (!employee.isEnabled()) {
            throw new EmailNotVerifiedException("Email not verified. Please check your email.");
        }

        matchPassword(loginRequest.getPassword(), employee.getPasswordHash());

        if (employee.isMfaEnabled()) {
            String preAuthToken = jwtUtil.generatePreAuthToken(employee);
            return LoginResponse.builder()
                    .accessToken(preAuthToken)
                    .message("MFA_REQUIRED")
                    .build();
        }


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

    public OtpResponse sendOtp(EmailOtpRequest request) {
        Employee employee = getEmployeeByUsername(request.getUsername());
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
        Employee employee = getEmployeeByUsername(request.getUsername());
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

    public List<String> generateAndSaveRecoveryCodes(Employee employee) {
        SecureRandom secureRandom = new SecureRandom();
        // 8 haneli rastgele sayısal kodlar
        List<String> plainCodes = IntStream.range(0, 8)
                .mapToObj(i -> String.format("%08d", secureRandom.nextInt(100000000)))
                .collect(Collectors.toList());
        //rastgele üretilen kodları hashleyip db ye kaydeder
        List<RecoveryCode> recoveryCodes = plainCodes.stream()
                .map(code -> RecoveryCode.builder()
                        .dbHashedRecoveryCode(passwordEncoder.encode(code))
                        .employee(employee)
                        .used(false)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        employee.getRecoveryCodes().clear();
        employee.getRecoveryCodes().addAll(recoveryCodes);
        repository.save(employee);
        // sadece kurulum anında kullanıcıya 1 kere gösterilmek üzere düz metin döner.
        return plainCodes;
    }

    public LoginResponse verifyRecoveryCode(String username, String recoveryCode) {
        Employee employee = getEmployeeByUsername(username);
        RecoveryCode matchedRecoveryCode = null;
        for (RecoveryCode dbHashedRecoverCode : employee.getRecoveryCodes()) {
            if (!dbHashedRecoverCode.isUsed() && passwordEncoder.matches(recoveryCode, dbHashedRecoverCode.getDbHashedRecoveryCode())) {
                matchedRecoveryCode = dbHashedRecoverCode;
                break;
            }
        }
        if (matchedRecoveryCode == null) {
            throw new BadCredentialsException("Recovery code is invalid");
        }
        employee.getRecoveryCodes().remove(matchedRecoveryCode);
        repository.save(employee);
        String accessToken = jwtUtil.generateToken(employee);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(username)
                .message("Login successfully with recovery code")
                .build();
    }

    public TotpSetupResponse setupTotp(String username) {
        Employee employee = getEmployeeByUsername(username);
        String secret = totpService.generateSecret();
        employee.setTempTotpSecret(secret);
        repository.save(employee);

        String qrImage = totpService.generateQrCodeImage(secret, employee.getUsername());
        return TotpSetupResponse.builder()
                .secret(secret)
                .qrCodeImage(qrImage)
                .build();
    }

    public MfaEnableResponse enableTotp(String username, String code) {
        Employee employee = getEmployeeByUsername(username);

        String secretToVerify = employee.getTempTotpSecret() != null ?
                employee.getTempTotpSecret() : employee.getTotpSecret();

        if (secretToVerify == null || !totpService.verifyCode(employee.getTotpSecret(), code)) {
            throw new BadCredentialsException("Invalid TOTP code");
        }
        employee.setTotpSecret(secretToVerify);
        employee.setTempTotpSecret(null);
        employee.setMfaEnabled(true);

        repository.save(employee);
        String message = "TOTP 2FA enabled successfully";
        List<String> recoveryCodes = generateAndSaveRecoveryCodes(employee);
        return MfaEnableResponse.builder()
                .message(message)
                .recoveryCodes(recoveryCodes)
                .build();
    }

    public LoginResponse verifyTotpLogin(String username, String code) {
        Employee employee = getEmployeeByUsername(username);
        if (!totpService.verifyCode(employee.getTotpSecret(), code)) {
            throw new BadCredentialsException("Invalid TOTP code");
        }

        String accessToken = jwtUtil.generateToken(employee);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee.getUsername());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(username)
                .message("Login successfully")
                .build();
    }

    private Employee getEmployeeByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
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

    private void checkIfTcExists(String tcNo) {
        if (repository.existsByTcNo(tcNo)) {
            throw new AlreadyExistsException("TC number already exists.");
        }
    }

    private void matchPassword(String loginPassword, String dbPasswordHashed) {
        if (!passwordEncoder.matches(loginPassword, dbPasswordHashed)) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
