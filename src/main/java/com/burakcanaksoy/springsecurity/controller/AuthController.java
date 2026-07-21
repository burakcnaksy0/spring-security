package com.burakcanaksoy.springsecurity.controller;

import com.burakcanaksoy.springsecurity.dto.request.*;
import com.burakcanaksoy.springsecurity.dto.response.*;
import com.burakcanaksoy.springsecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody EmployeeRegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody EmployeeLoginRequest loginRequest) {
        return ResponseEntity.ok().body(authService.login(loginRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid or missing Authorization header.");
        }
        String jwt = authorization.substring(7);
        return ResponseEntity.ok().body(authService.logout(jwt));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id){

        return ResponseEntity.ok(authService.delete(id));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token){
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest){
        return ResponseEntity.ok(authService.forgotPassword(forgotPasswordRequest));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token , @Valid @RequestBody ResetPasswordRequest resetPasswordRequest){
        return ResponseEntity.ok().body(authService.resetPassword(token,resetPasswordRequest));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody EmailOtpRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        return ResponseEntity.ok(authService.setupTotp(authentication.getName()));
    }

    @PostMapping("/totp/enable")
    public ResponseEntity<String> enableTotp(Authentication authentication, @RequestParam String code) {
        return ResponseEntity.ok(authService.enableTotp(authentication.getName(), code));
    }

    @PostMapping("/totp/verify-login")
    public ResponseEntity<LoginResponse> verifyTotpLogin(Authentication authentication , @RequestParam String code) {
        return ResponseEntity.ok(authService.verifyTotpLogin(authentication.getName(), code));
    }

    @PostMapping("/set-password")
    public ResponseEntity<String> setPassword(Authentication authentication, @RequestParam String newPassword) {
        return ResponseEntity.ok(authService.setPasswordForOAuthUser(authentication.getName(), newPassword));
    }
}
