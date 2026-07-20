package com.burakcanaksoy.springsecurity.config;

import com.burakcanaksoy.springsecurity.rule.RateLimitRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class RateLimitRulesConfig {

    @Bean
    public List<RateLimitRule> rateLimitRules() {
        return List.of(
                // 10 dakikada 3 login denemesi
                new RateLimitRule("/api/v1/auth/login", 3, Duration.ofMinutes(10)),

                // saatte 2 register
                new RateLimitRule("/api/v1/auth/register", 2, Duration.ofHours(1)),

                // 15 dakikada 5 OTP gönderme isteği
                new RateLimitRule("/api/v1/auth/send-otp", 5, Duration.ofMinutes(15)),

                // 5 dakikada 5 OTP doğrulama denemesi
                new RateLimitRule("/api/v1/auth/verify-otp", 5, Duration.ofMinutes(5)),

                // 5 dakikada 5 TOTP doğrulama denemesi
                new RateLimitRule("/api/v1/auth/totp/verify-login", 5, Duration.ofMinutes(5)),

                // saatte 30 istek
                new RateLimitRule("/api/v1/auth/refresh-token",30,Duration.ofHours(1)),

                // saatte 3 şifre sıfırlama isteği
                new RateLimitRule("/api/v1/auth/forgot-password", 3, Duration.ofHours(1))
        );
    }
}
