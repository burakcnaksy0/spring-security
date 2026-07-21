package com.burakcanaksoy.springsecurity.exception;

import com.burakcanaksoy.springsecurity.entity.AuthProvider;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.RefreshToken;
import com.burakcanaksoy.springsecurity.entity.Role;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.service.RefreshTokenService;
import com.burakcanaksoy.springsecurity.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final EmployeeRepository repository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String providerId = oauth2User.getAttribute("sub");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "email_not_provided");
            return;
        }

        Boolean emailVerified = oauth2User.getAttribute("email_verified");
        if (emailVerified == null || !emailVerified) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "email_not_verified_by_google");
            return;
        }

        Employee employee = repository.findByEmail(email)
                .map(existing -> linkGoogleIfNeeded(existing, providerId))
                .orElseGet(() -> registerNewOAuthEmployee(email, providerId, name));

        String accessToken = jwtUtil.generateToken(employee);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee.getUsername());

        // TEST AMAÇLI: redirect yerine direkt JSON yazıyoruz
        Map<String, String> body = new HashMap<>();
        body.put("accessToken", accessToken);
        body.put("refreshToken", refreshToken.getToken());
        body.put("username", employee.getUsername());
        body.put("email", employee.getEmail());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private Employee linkGoogleIfNeeded(Employee existing, String providerId) {
        boolean changed = false;

        if (existing.getProviderId() == null) {
            existing.setProviderId(providerId);
            changed = true;
        }
        if (existing.getProvider() == null) {
            existing.setProvider(AuthProvider.GOOGLE);
            changed = true;
        }
        if (!existing.isEnabled()) {
            existing.setEnabled(true);
            changed = true;
        }

        if (changed) {
            repository.save(existing);
        }
        return existing;
    }

    private Employee registerNewOAuthEmployee(String email, String providerId, String name) {
        Employee employee = Employee.builder()
                .email(email)
                .username(email)
                .firstName(name)
                .passwordHash(null)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .enabled(true)
                .mfaEnabled(false)
                .role(Role.USER)
                .build();
        return repository.save(employee);
    }
}