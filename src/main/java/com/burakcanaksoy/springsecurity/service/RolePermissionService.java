package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.dto.request.AssignPermissionsRequest;
import com.burakcanaksoy.springsecurity.dto.request.AssignRolesRequest;
import com.burakcanaksoy.springsecurity.dto.request.PermissionRequest;
import com.burakcanaksoy.springsecurity.dto.request.RoleRequest;
import com.burakcanaksoy.springsecurity.dto.response.EmployeeResponse;
import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.Permission;
import com.burakcanaksoy.springsecurity.entity.Role;
import com.burakcanaksoy.springsecurity.exception.AlreadyExistsException;
import com.burakcanaksoy.springsecurity.exception.ResourceNotFoundException;
import com.burakcanaksoy.springsecurity.mapper.EmployeeMapper;
import com.burakcanaksoy.springsecurity.repository.EmployeeRepository;
import com.burakcanaksoy.springsecurity.repository.PermissionRepository;
import com.burakcanaksoy.springsecurity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public Permission createPermission(PermissionRequest request) {
        if (permissionRepository.findByName(request.getName()).isPresent()) {
            throw new AlreadyExistsException("Permission with name " + request.getName() + " already exists.");
        }
        Permission permission = Permission.builder()
                .name(request.getName().toUpperCase())
                .build();
        return permissionRepository.save(permission);
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Role createRole(RoleRequest request) {
        String roleName = request.getName().toUpperCase();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }
        if (roleRepository.findByName(roleName).isPresent()) {
            throw new AlreadyExistsException("Role with name " + roleName + " already exists.");
        }
        Role role = Role.builder()
                .name(roleName)
                .permissions(new HashSet<>())
                .build();
        return roleRepository.save(role);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Role assignPermissionsToRole(Long roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
        role.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(role);
    }

    @Transactional
    public EmployeeResponse assignRolesToEmployee(Long employeeId, AssignRolesRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        List<Role> roles = roleRepository.findAllById(request.getRoleIds());
        employee.setRoles(new HashSet<>(roles));
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }
}
