package com.burakcanaksoy.springsecurity.dto.request;

import com.burakcanaksoy.springsecurity.annotation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeRegisterRequest {
    @NotBlank(message = "Username cannot be blank.")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
    private String username;

    @NotBlank(message = "Email cannot be blank.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email cannot exceed 100 characters.")
    private String email;

    @NotBlank(message = "Phone number cannot be blank.")
    @Pattern(
            regexp = "^05\\d{9}$",
            message = "Phone number must be in the format 05XXXXXXXXX."
    )
    private String phoneNumber;

    @NotBlank(message = "TC No cannot be blank.")
    @Pattern(
            regexp = "^[1-9][0-9]{10}$",
            message = "TC No must consist of exactly 11 digits and cannot start with 0."
    )
    private String tcNo;

    @NotBlank(message = "Password cannot be blank.")
    @Password
    private String password;
}
