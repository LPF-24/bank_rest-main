package com.example.bankcards.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Base64;

@Converter
public class PanAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String pan) {
        if (pan == null) return null;
        // Здесь должна быть настоящая шифрация
        return Base64.getEncoder().encodeToString(pan.getBytes());
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // Здесь должна быть расшифровка
        return new String(Base64.getDecoder().decode(dbData));
    }
}

