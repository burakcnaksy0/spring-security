package com.burakcanaksoy.springsecurity.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String tcNo;
    private LocalDate birthDate;
    private String gender;
    private String phoneNumber;
    private String email;
    private String address;
    private boolean enabled;
    private Set<String> roles;
    private Set<String> permissions;

}

