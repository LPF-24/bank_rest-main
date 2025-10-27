package com.example.bankcards.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;

@Converter
public class PanAttributeConverter implements AttributeConverter<String, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(String pan) {
        if (pan == null) return null;
        // TODO: заменить на реальную шифрацию (например, AES-GCM) и вернуть шифротекст
        return pan.getBytes(StandardCharsets.UTF_8); // временно ок
        // Если надо через Base64 как «заглушку» шифрования:
        // return Base64.getEncoder().encode(pan.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        // TODO: заменить на реальную расшифровку
        return new String(dbData, StandardCharsets.UTF_8);
        // Если выше кодировано в Base64:
        // return new String(Base64.getDecoder().decode(dbData), StandardCharsets.UTF_8);
    }
}