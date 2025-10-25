package com.example.bankcards.util.validator;

import com.example.bankcards.util.annotation.ValidDateOfBirth;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class DateOfBirthConstraintValidator implements ConstraintValidator<ValidDateOfBirth, LocalDate> {
    private static final Logger logger = LoggerFactory.getLogger(DateOfBirthConstraintValidator.class);

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext constraintValidatorContext) {
        logger.info("Method isValid of DateOfBirthConstraintValidator started");
        if (dateOfBirth == null) {
            return true;
        }
        logger.info("Middle of the method isValid of DateOfBirthConstraintValidator");

        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(14);
        logger.info("Method isValid of DateOfBirthConstraintValidator ended");
        return dateOfBirth.isBefore(today) && dateOfBirth.isBefore(minDate);
    }
}
