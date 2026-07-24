package com.burakcanaksoy.springsecurity.controller;

import com.burakcanaksoy.springsecurity.dto.request.EmployeeCreateRequest;
import com.burakcanaksoy.springsecurity.dto.response.EmployeeResponse;
import com.burakcanaksoy.springsecurity.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping("/all")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok().body(employeeService.getAllEmployees());
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id)
    {
        employeeService.deleteEmployee(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<EmployeeResponse> getEmployee() {
        return ResponseEntity.ok().body(employeeService.getEmployee());
    }

    @PutMapping
    public ResponseEntity<EmployeeResponse> updateEmployeeInformationAfterLogin(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.updateEmployeeInformationAfterLogin(request));
    }
}

