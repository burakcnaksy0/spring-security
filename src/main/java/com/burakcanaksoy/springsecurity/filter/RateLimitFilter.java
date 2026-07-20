package com.burakcanaksoy.springsecurity.filter;

import com.burakcanaksoy.springsecurity.rule.RateLimitRule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

// Belirli bir süre içerisinde bir kullanıcının veya IP'nin gönderebileceği istek sayısını sınırlandırmaktır.
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final List<RateLimitRule> rateLimitRules;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        RateLimitRule matchedRule = rateLimitRules.stream()
                .filter(rule -> pathMatcher.match(rule.getPathPattern(), path))
                .findFirst()
                .orElse(null);

        // bu path için tanımlı kural yoksa rate limit uygulanmaz
        if (matchedRule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = buildRateLimitKey(request, matchedRule);
        Bucket bucket = resolveBucket(rateLimitKey, matchedRule);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Too many requests for this action. Please try again later.\"}"
            );
        }
    }

    private Bucket resolveBucket(String key, RateLimitRule rule) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.getCapacity())
                        .refillIntervally(rule.getCapacity(), rule.getRefillDuration())
                        .build())
                .build();

        return proxyManager.builder().build(keyBytes, configSupplier);
    }

    // ENPOİNT VE IP kombinasyonu için key üretir.
    private String buildRateLimitKey(HttpServletRequest request, RateLimitRule rule) {
        String clientIp = extractClientIp(request);
        // key'e path'i de dahil ediyoruz ki aynı IP'nin login ve register hakları birbirine karışmasın
        return "rate-limit:" + rule.getPathPattern() + ":" + clientIp;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /*
    // ConcurrentHashMap yaklaşımında bucket JVM heapinde tutuluyor(application RAM).
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 1 dakikada 5 istek
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                // izin verilen istek sayısı
                .capacity(5)
                // refill bucket zamanla nasıl dolar;
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean shouldLimit = path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/send-otp")
                || path.equals("/api/v1/auth/verify-otp")
                || path.equals("/api/v1/auth/totp/verify-login")
                || path.equals("/api/v1/auth/forgot-password");

        if (!shouldLimit) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
        }
    }
    */

}