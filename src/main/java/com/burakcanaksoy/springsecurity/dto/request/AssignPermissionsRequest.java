package com.burakcanaksoy.springsecurity.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignPermissionsRequest {
    @NotEmpty(message = "Permission IDs cannot be empty")
    private Set<Long> permissionIds;
}
