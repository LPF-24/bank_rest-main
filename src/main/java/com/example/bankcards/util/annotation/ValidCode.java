package com.example.bankcards.util.annotation;

import com.example.bankcards.util.validator.CodeConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CodeConstraintValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCode {
    String message() default "Invalid code!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
