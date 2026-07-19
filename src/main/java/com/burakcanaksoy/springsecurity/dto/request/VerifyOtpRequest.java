package com.burakcanaksoy.springsecurity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Username cannot be blank.")
    private String username;

    @NotBlank(message = "OTP code cannot be blank.")
    @Size(min = 6, max = 6, message = "OTP code must be exactly 6 characters.")
    private String otpCode;
}
