package com.burakcanaksoy.springsecurity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeCreateRequest {
    @NotBlank(message = "Username cannot be blank.")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
    private String username;

    @NotBlank(message = "First name cannot be blank.")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters.")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank.")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters.")
    private String lastName;

    @NotBlank(message = "Email cannot be blank.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email cannot exceed 100 characters.")
    private String email;

    @NotBlank(message = "TC No cannot be blank.")
    @Pattern(
            regexp = "^[1-9][0-9]{10}$",
            message = "TC No must consist of exactly 11 digits and cannot start with 0."
    )
    private String tcNo;

    @NotBlank(message = "Phone number cannot be blank.")
    @Pattern(
            regexp = "^05\\d{9}$",
            message = "Phone number must be in the format 05XXXXXXXXX."
    )
    private String phoneNumber;

    @NotNull(message = "Birth date cannot be null.")
    @Past(message = "Birth date must be in the past.")
    private LocalDate birthDate;

    @NotBlank(message = "Gender cannot be blank.")
    @Pattern(
            regexp = "^(MALE|FEMALE)$",
            message = "Gender must be MALE or FEMALE."
    )
    private String gender;

    @NotBlank(message = "Address cannot be blank.")
    @Size(min = 10, max = 255, message = "Address must be between 10 and 255 characters.")
    private String address;
}