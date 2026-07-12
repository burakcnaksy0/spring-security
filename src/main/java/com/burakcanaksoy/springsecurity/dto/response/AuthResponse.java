package com.burakcanaksoy.springsecurity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuthResponse {
    private String username;
    private String role;
    private String email;
    private String phone;
    private String tc;
}
