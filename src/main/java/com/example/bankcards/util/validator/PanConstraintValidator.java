package com.example.bankcards.util.validator;

import com.example.bankcards.util.annotation.ValidPan;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PanConstraintValidator implements ConstraintValidator<ValidPan, String> {
    @Override
    public boolean isValid(String pan, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (pan == null || !pan.matches("\\d{13,19}")) {
            context.buildConstraintViolationWithTemplate("PAN must contain only digits and length between 13 and 19 characters")
                    .addConstraintViolation();
            return false;
        }

        if (pan.startsWith("0")) {
            context.buildConstraintViolationWithTemplate("PAN can't starts with 0")
                    .addConstraintViolation();
            return false;
        }

        return isLuhnValid(pan);
    }

    private boolean isLuhnValid(String pan) {
        int sum = 0;
        boolean alternate = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(pan.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
