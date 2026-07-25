package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.*;
import com.burakcanaksoy.springsecurity.dto.response.AuthResponse;
import com.burakcanaksoy.springsecurity.dto.response.LoginResponse;
import com.burakcanaksoy.springsecurity.dto.response.RefreshTokenResponse;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.Role;
import com.burakcanaksoy.springsecurity.entity.PasswordResetToken;
import com.burakcanaksoy.springsecurity.entity.RefreshToken;
import com.burakcanaksoy.springsecurity.entity.VerificationToken;
import com.burakcanaksoy.springsecurity.exception.AlreadyExistsException;
import com.burakcanaksoy.springsecurity.exception.EmailNotVerifiedException;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.mapper.EmployeeMapper;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.repository.RoleRepository;
import com.burakcanaksoy.springsecurity.security.CustomUserPrincipal;
import com.burakcanaksoy.springsecurity.util.CookieUtil;
import com.burakcanaksoy.springsecurity.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmployeeRepository repository;
    private final RoleRepository roleRepository;
    private final EmployeeMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final CookieUtil cookieUtil;

    public AuthResponse register(EmployeeRegisterRequest registerRequest) {
        checkIfUsernameExists(registerRequest.getUsername());
        checkIfEmailExists(registerRequest.getEmail());
        checkIfPhoneExists(registerRequest.getPhoneNumber());
        checkIfTcExists(registerRequest.getTcNo());

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        Employee employee = mapper.toEmployee(registerRequest, Set.of(defaultRole));
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

    public LoginResponse login(EmployeeLoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        Employee employee = repository.findByUsername(loginRequest.getUsername()).orElseThrow(() -> new ResourceNotFoundException("Employee not found with this username : " + loginRequest.getUsername()));

        if (!employee.isEnabled()) {
            throw new EmailNotVerifiedException("Email not verified. Please check your email.");
        }

        matchPassword(loginRequest.getPassword(), employee.getPasswordHash());

        String accessToken = jwtUtil.generateToken(employee);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee.getUsername());

        cookieUtil.addAccessTokenCookie(response, accessToken, 15 * 60);
        cookieUtil.addRefreshTokenCookie(response, refreshToken.getToken(), 7 * 24 * 60);

        //  POST, PUT, PATCH, DELETE
        // Spring Security CSRF kontrolünü SADECE bu metotlarda zorunlu kılar.
        // CSRF saldırısı için XSRF-TOKEN cookie üretir.
        CsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);

        return LoginResponse.builder()
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

    public RefreshTokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieUtil.extractTokenFromCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue == null) {
            throw new RuntimeException("Refresh token not found");
        }
        return refreshTokenService.findByToken(refreshTokenValue)
                .map(refreshTokenService::verifyRefreshToken)
                .map(RefreshToken::getEmployee)
                .map(employee -> {
                    String accessToken = jwtUtil.generateToken(employee);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(employee.getUsername());

                    cookieUtil.addAccessTokenCookie(response, accessToken, 15 * 60);
                    cookieUtil.addRefreshTokenCookie(response, newRefreshToken.getToken(), 7 * 24 * 60);

                    return RefreshTokenResponse.builder()
                            .message("Refresh successful")
                            .build();
                }).orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    public String logout(HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            refreshTokenService.deleteEmployeeId(principal.getId());
            cookieUtil.clearAuthCookies(response);
            return "Logout successfully with username : " + principal.getUsername();
        }
        cookieUtil.clearAuthCookies(response);
        return "Logout successfully";
    }

    public String forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        Optional<Employee> employeeOptional = repository.findByEmail(forgotPasswordRequest.getEmail());
        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            String passwordResetToken = passwordResetTokenService.createPasswordResetToken(employee);
            emailService.sendPasswordReset(employee, passwordResetToken);
        }
        return "If this email address is registered, a password reset email has been sent.";
    }

    @Transactional
    public String resetPassword(String token, ResetPasswordRequest resetPasswordRequest) {
        PasswordResetToken passwordResetToken = passwordResetTokenService.getByRawToken(token);

        if (passwordResetToken.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenService.deleteToken(passwordResetToken);
            throw new RuntimeException("Password reset token has expired. Please request again.");
        }
        Employee employee = passwordResetToken.getEmployee();
        employee.setPasswordHash(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        repository.save(employee);
        passwordResetTokenService.deleteToken(passwordResetToken);
        return "Your password has been reset successfully.";
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

}

/*
Analyze this project thoroughly and ensure that when I run 'docker-compose up -d', the entire system starts up completely.
The docker-compose configuration should handle starting all necessary services including frontend, backend, database, and any other required components.
Review the current docker-compose.yml file and the overall project structure to ensure all services are properly defined and can run together seamlessly.
 The system should be fully operational after the docker-compose command executes.
 */