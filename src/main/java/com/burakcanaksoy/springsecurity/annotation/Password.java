package com.burakcanaksoy.springsecurity.annotation;

import com.burakcanaksoy.springsecurity.validator.PasswordValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD,ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {PasswordValidation.class})
public @interface Password {
    String message() default "{The password must be at least 8 characters long and include uppercase letters, lowercase letters, numbers, and special characters.}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
