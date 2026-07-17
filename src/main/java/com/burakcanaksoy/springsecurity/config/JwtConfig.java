package com.burakcanaksoy.springsecurity.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secretKey;
    private long accessExpiration;
    private long refreshExpiration;
    private long verificationExpiration;
}
