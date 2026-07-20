package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    private static final String PREFIX = "blacklist:token:";

    public void blacklistToken(String jwt, long remainingValidityMillis) {
        if (remainingValidityMillis <= 0) {
            return;
        }
        String key = key(jwt);
        stringRedisTemplate.opsForValue().set(key, "true", Duration.ofMillis(remainingValidityMillis));
    }

    public boolean isBlacklisted(String jwt) {
        String key = key(jwt);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private String key(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT cannot be null");
        }
        String jti = jwtUtil.extractJti(token);
        return PREFIX + jti;
    }
}
