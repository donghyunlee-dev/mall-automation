package com.sfood.automation.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    // 지연 조회: AesGcmEncryptor 빈이 없는 JPA 슬라이스 테스트에서도 컨텍스트 로드가 가능해야 한다
    private final ObjectProvider<AesGcmEncryptor> encryptorProvider;

    public EncryptedFieldConverter(ObjectProvider<AesGcmEncryptor> encryptorProvider) {
        this.encryptorProvider = encryptorProvider;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptorProvider.getObject().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptorProvider.getObject().decrypt(dbData);
    }
}
