package com.example.bankcards.util.annotation;

import com.example.bankcards.util.validator.PanConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PanConstraintValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPan {
    String message() default "Invalid PAN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
