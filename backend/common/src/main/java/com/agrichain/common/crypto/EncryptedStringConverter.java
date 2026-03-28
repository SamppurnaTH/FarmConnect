package com.agrichain.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts PII String fields
 * using AES-256-GCM via {@link AesEncryptionUtil}.
 *
 * Usage on entity field:
 *   {@code @Convert(converter = EncryptedStringConverter.class)}
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    // Static reference set by Spring so the converter can be used by JPA
    private static AesEncryptionUtil encryptionUtil;

    @Autowired
    public void setEncryptionUtil(AesEncryptionUtil util) {
        EncryptedStringConverter.encryptionUtil = util;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (encryptionUtil == null || attribute == null) {
            return attribute;
        }
        return encryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (encryptionUtil == null || dbData == null) {
            return dbData;
        }
        return encryptionUtil.decrypt(dbData);
    }
}
