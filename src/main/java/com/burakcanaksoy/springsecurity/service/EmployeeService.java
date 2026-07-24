package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.EmployeeCreateRequest;
import com.burakcanaksoy.springsecurity.dto.response.EmployeeResponse;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.mapper.EmployeeMapper;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    private final com.burakcanaksoy.springsecurity.repository.VerificationTokenRepository verificationTokenRepository;
    private final com.burakcanaksoy.springsecurity.repository.RefreshTokenRepository refreshTokenRepository;
    private final com.burakcanaksoy.springsecurity.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    public List<EmployeeResponse> getAllEmployees() {
        List<Employee> employeeList = repository.findAll();
        return mapper.toResponseList(employeeList);
    }

    public EmployeeResponse getEmployee() {
        Employee employee = authenticationEmployee();
        return mapper.toResponse(employee);
    }


    public EmployeeResponse updateEmployeeInformationAfterLogin(EmployeeCreateRequest request) {
        Employee employee = authenticationEmployee();
        Employee updateEmployeeRequest = mapper.toUpdateEmployeeRequest(employee, request);
        Employee saved = repository.save(updateEmployeeRequest);
        return mapper.toResponse(saved);

    }

    @jakarta.transaction.Transactional
    public void deleteEmployee(Long id) {
        Employee employee = getById(id);
        verificationTokenRepository.deleteByEmployeeId(id);
        refreshTokenRepository.deleteByEmployeeId(id);
        passwordResetTokenRepository.deleteByEmployeeId(id);
        repository.delete(employee);
    }

    private Employee getById(Long id) {
        Employee employee = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found with this id : " + id));
        return employee;
    }

    private Employee authenticationEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        Employee employee = getById(principal.getId());
        return employee;
    }
}
