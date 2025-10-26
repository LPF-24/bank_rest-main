package com.example.bankcards.util.validator;

import com.example.bankcards.util.annotation.ValidCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CodeConstraintValidator implements ConstraintValidator<ValidCode, String> {
    @Value("${app.security.admin-activation-code}")
    private String ADMIN_ACTIVATION_CODE;

    @Override
    public boolean isValid(String code, ConstraintValidatorContext context) {
        if (code == null || code.isBlank()) {
            return false;
        }

        return code.equals(ADMIN_ACTIVATION_CODE);
    }
}
