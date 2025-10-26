package com.example.bankcards.util.validator;

import com.example.bankcards.util.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.passay.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {
    private static final Logger logger = LoggerFactory.getLogger(PasswordConstraintValidator.class);

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true;
        }

        logger.error("Password annotation triggered for: {}", password);

        PasswordValidator validator = new PasswordValidator(List.of(
                new LengthRule(8, 30),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new WhitespaceRule()
        ));

        RuleResult result = validator.validate(new PasswordData(password));

        if (result.isValid()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                String.join(", ", validator.getMessages(result))
        ).addConstraintViolation();

        return false;
    }
}
