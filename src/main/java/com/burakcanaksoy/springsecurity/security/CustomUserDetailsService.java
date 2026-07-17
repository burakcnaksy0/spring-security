package com.burakcanaksoy.springsecurity.security;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;

import java.util.*;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final EmployeeRepository repository;

    // Spring login olduğunda otomatik bunu çağırır.
    // Kullanıcıyı veri kaynağından (veritabanı, LDAP, API vb.) bulup UserDetails nesnesi olarak döndürür.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = repository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("Employee not found with this username : " + username));
        return CustomUserPrincipal.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .email(employee.getEmail())
                .password(employee.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())))
                .build();
    }
}
/*
new CustomUserPrincipal(
                employee.getId(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getPasswordHash(),
                List.of(
                        new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())
                )
 */