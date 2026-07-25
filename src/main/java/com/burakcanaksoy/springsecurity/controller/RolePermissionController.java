package com.burakcanaksoy.springsecurity.controller;

import com.burakcanaksoy.springsecurity.dto.request.AssignPermissionsRequest;
import com.burakcanaksoy.springsecurity.dto.request.AssignRolesRequest;
import com.burakcanaksoy.springsecurity.dto.request.PermissionRequest;
import com.burakcanaksoy.springsecurity.dto.request.RoleRequest;
import com.burakcanaksoy.springsecurity.dto.response.EmployeeResponse;
import com.burakcanaksoy.springsecurity.entity.Permission;
import com.burakcanaksoy.springsecurity.entity.Role;
import com.burakcanaksoy.springsecurity.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping("/permissions")
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolePermissionService.createPermission(request));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllPermissions());
    }

    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolePermissionService.createRole(request));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(rolePermissionService.getAllRoles());
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<Role> assignPermissionsToRole(
            @PathVariable Long roleId,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(rolePermissionService.assignPermissionsToRole(roleId, request));
    }

    @PostMapping("/employees/{employeeId}/roles")
    public ResponseEntity<EmployeeResponse> assignRolesToEmployee(
            @PathVariable Long employeeId,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(rolePermissionService.assignRolesToEmployee(employeeId, request));
    }
}
