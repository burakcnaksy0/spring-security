package com.burakcanaksoy.springsecurity.dto.request;

import com.burakcanaksoy.springsecurity.annotation.Password;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Password cannot be blank.")
    @Password
    private String newPassword;
}
