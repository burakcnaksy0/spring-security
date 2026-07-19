package com.burakcanaksoy.springsecurity.filter;

import com.burakcanaksoy.springsecurity.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.*;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            username = jwtUtil.extractUsername(jwt);
            // burdan sonrası performans&güvenlik ilişkisini belirler.
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // burada bi kere daha db ye istek atılır. db içindeki güncel bilgiler alınır.
                // bu yapılmak istenmezse ; yukardaki jwt içinden id , username , mail vs gibi bilgileri alıp UserDetails nesnesini kendimiz oluşturup
                // UsernamePasswordAuthenticationToken'nın parametre alanına ekleriz. böylece performans artarken güvenlik açığı veririz.(db den güncel bilgiler çekilmedi.)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    boolean mfaPending = jwtUtil.isMfaPending(jwt);
                    List<GrantedAuthority> authorities;
                    if (mfaPending) {
                        // gerçek roller değil, sadece TOTP doğrulama endpoint'ine erişim yetkisi
                        authorities = List.of(new SimpleGrantedAuthority("ROLE_PRE_AUTH"));
                    } else {
                        authorities = List.copyOf(userDetails.getAuthorities());
                    }
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }catch (Exception e) {
            logger.error("JWT validation failed: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }


}
