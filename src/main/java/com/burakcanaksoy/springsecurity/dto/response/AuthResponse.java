package com.burakcanaksoy.springsecurity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuthResponse {
    private String username;
    private Set<String> roles;
    private String email;
    private String phone;
    private String tc;
    private boolean enabled;
}
