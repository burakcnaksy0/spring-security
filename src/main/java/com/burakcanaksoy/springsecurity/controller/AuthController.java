package com.burakcanaksoy.springsecurity.controller;

import com.burakcanaksoy.springsecurity.dto.request.EmployeeLoginRequest;
import com.burakcanaksoy.springsecurity.dto.request.EmployeeRegisterRequest;
import com.burakcanaksoy.springsecurity.dto.response.AuthResponse;
import com.burakcanaksoy.springsecurity.dto.response.LoginResponse;
import com.burakcanaksoy.springsecurity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody EmployeeRegisterRequest registerRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody EmployeeLoginRequest loginRequest){
        return ResponseEntity.ok().body(authService.login(loginRequest));
    }
}
