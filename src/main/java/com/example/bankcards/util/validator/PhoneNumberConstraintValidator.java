package com.example.bankcards.util.validator;

import com.example.bankcards.util.annotation.ValidPhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberConstraintValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null) return true;

        // Recommended when you want to collect multiple validation errors instead of stopping at the first.
        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (!phoneNumber.startsWith("+")) {
            context.buildConstraintViolationWithTemplate("Phone number must start with +")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!phoneNumber.matches("\\+?[0-9\\- ]{7,20}")) {
            context.buildConstraintViolationWithTemplate("The phone number must contain between 7 and 20 characters")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
