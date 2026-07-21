package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    // redis db ile haberleşmek için kullanılır.
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    private static final String PREFIX = "blacklist:token:";

    public void blacklistToken(String jwt, long remainingValidityMillis) {
        if (remainingValidityMillis <= 0) {
            return;
        }
        try {
            String key = key(jwt);
            log.info("TOKEN-BLACKLIST-KEY : {}", key);
            stringRedisTemplate.opsForValue().set(key, "true", Duration.ofMillis(remainingValidityMillis));
        } catch (Exception e) {
            log.error("Redis connection failed while blacklisting token. Token could not be saved to blacklist.", e);
        }
    }

    public boolean isBlacklisted(String jwt) {
        try {
            String key = key(jwt);
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis connection failed while blacklisting token. Token could not be saved to blacklist.", e);
        }
        return false;
    }

    private String key(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT cannot be null");
        }
        String jti = jwtUtil.extractJti(token);
        return PREFIX + jti;
    }
}
