package com.example.bankcards.util;

import java.security.SecureRandom;

public final class PanGenerator {
    private static final SecureRandom RND = new SecureRandom();
    private PanGenerator() {}

    /**
     * Генерирует валидный 16-значный PAN по BIN (6 цифр).
     */
    public static String generatePan(String bin6) {
        if (bin6 == null || bin6.length() != 6 || !bin6.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("BIN must be 6 digits");
        }
        StringBuilder pan = new StringBuilder(16);
        pan.append(bin6);
        // заполняем позиции 7..15-1 (т.е. ещё 9 цифр кроме контрольной)
        while (pan.length() < 15) {
            pan.append(RND.nextInt(10));
        }
        // считаем контрольную по Luhn
        int check = luhnCheckDigit(pan.toString());
        pan.append(check);
        return pan.toString();
    }

    private static int luhnCheckDigit(String first15) {
        int sum = 0;
        for (int i = 0; i < first15.length(); i++) {
            int digit = first15.charAt(first15.length() - 1 - i) - '0';
            if (i % 2 == 0) { // удваиваем каждую вторую с конца (для 0-based от конца)
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }
}
