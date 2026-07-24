package com.burakcanaksoy.springsecurity.controller;

import com.burakcanaksoy.springsecurity.dto.request.EmployeeLoginRequest;
import com.burakcanaksoy.springsecurity.dto.request.EmployeeRegisterRequest;
import com.burakcanaksoy.springsecurity.dto.request.ForgotPasswordRequest;
import com.burakcanaksoy.springsecurity.dto.request.ResetPasswordRequest;
import com.burakcanaksoy.springsecurity.dto.response.AuthResponse;
import com.burakcanaksoy.springsecurity.dto.response.LoginResponse;
import com.burakcanaksoy.springsecurity.dto.response.RefreshTokenResponse;
import com.burakcanaksoy.springsecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody EmployeeLoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok().body(authService.login(loginRequest, request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.refreshToken(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        return ResponseEntity.ok().body(authService.logout(response));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        return ResponseEntity.ok(authService.forgotPassword(forgotPasswordRequest));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        return ResponseEntity.ok().body(authService.resetPassword(token, resetPasswordRequest));
    }

}
