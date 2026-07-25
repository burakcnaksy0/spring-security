package com.burakcanaksoy.springsecurity.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignRolesRequest {
    @NotEmpty(message = "Role IDs cannot be empty")
    private Set<Long> roleIds;
}
