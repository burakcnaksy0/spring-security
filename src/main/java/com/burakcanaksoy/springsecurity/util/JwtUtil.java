package com.burakcanaksoy.springsecurity.util;

import com.burakcanaksoy.springsecurity.config.JwtConfig;
import com.burakcanaksoy.springsecurity.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtConfig jwtConfig;

    private Key getSigningKey() {
        byte[] bytes = Decoders.BASE64.decode(jwtConfig.getSecretKey());
        return Keys.hmacShaKeyFor(bytes);
    }

    // burda ekstra bilgiler eklemek perfomansı artırır ama güvenliği azaltabilir!
    public String generateToken(Employee employee) {
        Map<String, Object> claims = new HashMap<>();
        java.util.Set<String> roles = employee.getRoles().stream()
                .map(com.burakcanaksoy.springsecurity.entity.Role::getName)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> permissions = employee.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.burakcanaksoy.springsecurity.entity.Permission::getName)
                .collect(java.util.stream.Collectors.toSet());

        claims.put("email", employee.getEmail());
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("employeeId", employee.getId());
        return createToken(claims, employee.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public Long extractId(String token){
        return extractClaim(token, claim -> claim.get("employeeId", Long.class));
    }
    public String extractUsername(String token) {
        //return extractAllClaims(token).getSubject();
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        //return extractAllClaims(token).getExpiration();
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date(System.currentTimeMillis()));
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }


}
