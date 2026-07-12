package com.burakcanaksoy.springsecurity.mapper;

import com.burakcanaksoy.springsecurity.dto.request.EmployeeCreateRequest;
import com.burakcanaksoy.springsecurity.dto.request.EmployeeRegisterRequest;
import com.burakcanaksoy.springsecurity.dto.response.AuthResponse;
import com.burakcanaksoy.springsecurity.dto.response.EmployeeResponse;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmployeeMapper {
    private final PasswordEncoder passwordEncoder;

    public Employee toEmployee(EmployeeRegisterRequest request) {
        if (request == null) {
            return null;
        }
        return Employee.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .tcNo(request.getTcNo())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .role(Role.USER)
                .build();
    }


    public Employee toUpdateEmployeeRequest(Employee employee , EmployeeCreateRequest request){
        if (employee == null || request == null){
            return null;
        }
        employee.setUsername(request.getUsername());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setTcNo(request.getTcNo());
        employee.setBirthDate(request.getBirthDate());
        employee.setGender(request.getGender());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setEmail(request.getEmail());
        employee.setAddress(request.getAddress());
        return employee;
    }

    public AuthResponse toAuthResponse(Employee employee) {
        if (employee == null) {
            return null;
        }
        return AuthResponse.builder()
                .username(employee.getUsername())
                .role(employee.getRole().toString())
                .email(employee.getEmail())
                .phone(employee.getPhoneNumber())
                .tc(employee.getTcNo())
                .build();
    }

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }
        return EmployeeResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .tcNo(employee.getTcNo())
                .birthDate(employee.getBirthDate())
                .gender(employee.getGender())
                .phoneNumber(employee.getPhoneNumber())
                .email(employee.getEmail())
                .address(employee.getAddress())
                .role(employee.getRole().name())
                .build();
    }

    public List<EmployeeResponse> toResponseList(List<Employee> employeeList) {
        if (employeeList.isEmpty()) {
            return List.of();
        }
        return employeeList.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


}

