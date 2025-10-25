package com.example.bankcards.util.annotation;

import com.example.bankcards.util.validator.DateOfBirthConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateOfBirthConstraintValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateOfBirth {
    String message() default "Date of birth must be in the past. User must be over 14 years old.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
