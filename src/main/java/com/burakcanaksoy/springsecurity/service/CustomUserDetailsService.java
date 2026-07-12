package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.security.CustomUserPrincipal;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = repository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("Employee not found with this username : " + username));
        return new CustomUserPrincipal(
                employee.getId(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getPasswordHash(),
                List.of(
                        new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())
                )
        );
    }
}
