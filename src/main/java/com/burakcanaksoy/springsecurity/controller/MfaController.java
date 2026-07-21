package com.burakcanaksoy.springsecurity.controller;


import com.burakcanaksoy.springsecurity.dto.response.MfaEnableResponse;
import com.burakcanaksoy.springsecurity.dto.response.TotpSetupResponse;
import com.burakcanaksoy.springsecurity.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
public class MfaController {
    private final AuthService authService;

    @PostMapping("/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        return ResponseEntity.ok(authService.setupTotp(authentication.getName()));
    }

    @PostMapping("/enable")
    public ResponseEntity<MfaEnableResponse> enableTotp(Authentication authentication, @RequestParam String code) {
        return ResponseEntity.ok(authService.enableTotp(authentication.getName(), code));
    }
}