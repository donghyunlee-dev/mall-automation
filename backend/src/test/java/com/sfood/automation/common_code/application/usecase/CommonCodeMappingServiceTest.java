package com.sfood.automation.common_code.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeEntity;
import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonCodeMappingServiceTest {

    @Mock
    private CommonCodeJpaRepository repo;

    private CommonCodeMappingService service;

    @BeforeEach
    void setUp() {
        CommonCodeEntity s1 = entity("SERVICE", "업태", "046", "식품");
        CommonCodeEntity s2 = entity("SERVICE", "업태", "001", "가구");
        CommonCodeEntity i1 = entity("ITEM", "업종", "0037", "각종식품");
        CommonCodeEntity i2 = entity("ITEM", "업종", "9999", "기타");
        when(repo.findByUseYn("Y")).thenReturn(List.of(s1, s2, i1, i2));
        service = new CommonCodeMappingService(repo);
        service.loadCache();
    }

    @Test
    void findServiceCode_exactMatch() {
        assertThat(service.findServiceCode("식품")).isEqualTo(Optional.of("046"));
    }

    @Test
    void findServiceCode_trimAndCaseInsensitive() {
        assertThat(service.findServiceCode("  식품  ")).isEqualTo(Optional.of("046"));
    }

    @Test
    void findServiceCode_noMatch() {
        assertThat(service.findServiceCode("없는업태")).isEmpty();
    }

    @Test
    void findServiceCode_null() {
        assertThat(service.findServiceCode(null)).isEmpty();
    }

    @Test
    void findItemCode_exactMatch() {
        assertThat(service.findItemCode("각종식품")).isEqualTo(Optional.of("0037"));
    }

    @Test
    void findItemCode_noMatch() {
        assertThat(service.findItemCode("없는업종")).isEmpty();
    }

    private CommonCodeEntity entity(String groupCode, String groupName, String code, String codeName) {
        CommonCodeEntity e = new CommonCodeEntity();
        e.setGroupCode(groupCode);
        e.setGroupName(groupName);
        e.setCode(code);
        e.setCodeName(codeName);
        e.setUseYn("Y");
        e.setCreatedAt("2026-07-03 00:00:00");
        e.setUpdatedAt("2026-07-03 00:00:00");
        return e;
    }
}
