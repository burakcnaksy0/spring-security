package com.burakcanaksoy.springsecurity.security;

import com.burakcanaksoy.springsecurity.entity.Employee;
import com.burakcanaksoy.springsecurity.entity.LeaveRequest;
import com.burakcanaksoy.springsecurity.entity.enums.Role;
import com.burakcanaksoy.springsecurity.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {
    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null) {
            return false;
        }
        if (targetDomainObject instanceof LeaveRequest leaveRequest) {
            return checkLeaveRequestPermission(authentication, leaveRequest, permission.toString());
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || targetId == null) {
            return false;
        }
        if ("LeaveRequest".equals(targetType)) {
            Optional<LeaveRequest> leaveRequest = leaveRequestRepository.findById((Long) targetId);
            if (leaveRequest.isEmpty()) {
                return false;
            }
            return checkLeaveRequestPermission(authentication, leaveRequest.get(), permission.toString());
        }
        return false;
    }

    private boolean checkLeaveRequestPermission(Authentication authentication, LeaveRequest leaveRequest, String permission) {
        Long principalId = null;
        boolean isAdmin = false;

        Object principalObj = authentication.getPrincipal();
        if (principalObj instanceof CustomUserPrincipal customUserPrincipal) {
            principalId = customUserPrincipal.getId();
            isAdmin = customUserPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        } else if (principalObj instanceof Employee employee) {
            principalId = employee.getId();
            isAdmin = employee.getRole() == Role.ADMIN;
        }

        if (principalId == null) {
            return false;
        }

        boolean isOwner = leaveRequest.getEmployee() != null && leaveRequest.getEmployee().getId().equals(principalId);

        return switch (permission) {
            case "READ" -> isOwner || isAdmin;
            case "WRITE" -> isOwner;
            case "DELETE" -> isAdmin || isOwner;
            case "APPROVE" -> isAdmin;
            default -> false;
        };
    }
}
