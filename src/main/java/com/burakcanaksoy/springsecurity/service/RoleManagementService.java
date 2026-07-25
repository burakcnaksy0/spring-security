package com.burakcanaksoy.springsecurity.service;

import com.burakcanaksoy.springsecurity.entity.Permission;
import com.burakcanaksoy.springsecurity.entity.Role;
import com.burakcanaksoy.springsecurity.repository.PermissionRepository;
import com.burakcanaksoy.springsecurity.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleManagementService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public void addPermissionToRole(String roleName, String permissionName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        Permission permission = permissionRepository.findByName(permissionName).orElseThrow();
        role.getPermissions().add(permission);
        roleRepository.save(role);
    }
}