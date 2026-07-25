package com.burakcanaksoy.springsecurity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionRequest {
    @NotBlank(message = "Permission name cannot be blank")
    private String name;
}
