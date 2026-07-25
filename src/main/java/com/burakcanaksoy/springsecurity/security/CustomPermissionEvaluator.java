package com.burakcanaksoy.springsecurity.security;

import com.burakcanaksoy.springsecurity.entity.LeaveRequest;
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

    private boolean checkLeaveRequestPermission(Authentication authentication, LeaveRequest leaveRequest, String permissionStr) {
        if (!(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return false;
        }

        boolean isOwner = leaveRequest.getEmployee() != null && leaveRequest.getEmployee().getId().equals(principal.getId());
        
        boolean hasSpecificPermission = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(permissionStr)
                        || a.getAuthority().equalsIgnoreCase("LEAVE_" + permissionStr));

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return switch (permissionStr.toUpperCase()) {
            case "READ" -> isOwner || isAdmin || hasSpecificPermission;
            case "WRITE" -> isOwner || hasSpecificPermission;
            case "DELETE" -> isAdmin || isOwner || hasSpecificPermission;
            case "APPROVE" -> isAdmin || hasSpecificPermission;
            default -> hasSpecificPermission;
        };
    }
}
