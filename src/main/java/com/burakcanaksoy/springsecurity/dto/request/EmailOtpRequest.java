package com.burakcanaksoy.springsecurity.dto.request;

import com.burakcanaksoy.springsecurity.annotation.Password;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailOtpRequest {

    @NotBlank(message = "Username cannot be blank.")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters.")
    private String username;

    @NotBlank(message = "Password cannot be blank.")
    @Password
    private String password;
}
