package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.EmployeeLoginRequest;
import com.burakcanaksoy.springsecurity.dto.request.EmployeeRegisterRequest;
import com.burakcanaksoy.springsecurity.dto.response.AuthResponse;
import com.burakcanaksoy.springsecurity.dto.response.LoginResponse;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.exception.AlreadyExistsException;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.mapper.EmployeeMapper;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.security.CustomUserPrincipal;
import com.burakcanaksoy.springsecurity.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(EmployeeRegisterRequest registerRequest) {
        checkIfUsernameExists(registerRequest.getUsername());
        checkIfEmailExists(registerRequest.getEmail());
        checkIfPhoneExists(registerRequest.getPhoneNumber());
        checkIfTcExists(registerRequest.getTcNo());
        Employee employee = mapper.toEmployee(registerRequest);
        Employee saved = repository.save(employee);
        return mapper.toAuthResponse(saved);
    }


    public LoginResponse login(EmployeeLoginRequest loginRequest) {
        Employee employee = repository.findByUsername(loginRequest.getUsername()).orElseThrow(() -> new ResourceNotFoundException("Employee not found with this username : " + loginRequest.getUsername()));
        matchPassword(loginRequest.getPassword(), employee.getPasswordHash());

        String token = jwtUtil.generateToken(employee);
        return LoginResponse.builder()
                .token(token)
                .username(employee.getUsername())
                .message("Login successfully")
                .build();
    }


    /*
    public LoginResponse login(EmployeeLoginRequest loginRequest) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        CustomUserPrincipal principal = (CustomUserPrincipal) authenticate.getPrincipal();
        String token = jwtUtil.generateToken(principal);
        return LoginResponse.builder()
                .token(token)
                .username(principal.getUsername())
                .message("Login successfully")
                .build();

    }
     */

    private void checkIfEmailExists(String email) {
        if (repository.existsByEmail(email)) {
            throw new AlreadyExistsException("Email already exists.");
        }
    }

    private void checkIfUsernameExists(String username) {
        if (repository.existsByUsername(username)) {
            throw new AlreadyExistsException("Username already exists.");
        }
    }

    private void checkIfPhoneExists(String phone) {
        if (repository.existsByPhoneNumber(phone)) {
            throw new AlreadyExistsException("Phone number already exists.");
        }
    }

    private void checkIfTcExists(String tcno) {
        if (repository.existsByTcNo(tcno)) {
            throw new AlreadyExistsException("TC number already exists.");
        }
    }

    private void matchPassword(String loginPassword, String dbPasswordHashed) {
        if (!passwordEncoder.matches(loginPassword, dbPasswordHashed)) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

}
