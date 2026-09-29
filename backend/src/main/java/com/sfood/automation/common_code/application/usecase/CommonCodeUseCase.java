package com.sfood.automation.common_code.application.usecase;

import com.sfood.automation.common_code.application.dto.CommonCodeListResponse;
import com.sfood.automation.common_code.application.dto.CommonCodeRequest;
import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeEntity;
import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeJpaRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonCodeUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CommonCodeJpaRepository commonCodeJpaRepository;

    public CommonCodeUseCase(CommonCodeJpaRepository commonCodeJpaRepository) {
        this.commonCodeJpaRepository = commonCodeJpaRepository;
    }

    public CommonCodeListResponse findAll(String groupCode, String useYn) {
        List<CommonCodeEntity> entities;
        if (groupCode != null && useYn != null) {
            entities = commonCodeJpaRepository.findByGroupCodeAndUseYnOrderByCodeAsc(groupCode, useYn);
        } else if (groupCode != null) {
            entities = commonCodeJpaRepository.findByGroupCodeOrderByCodeAsc(groupCode);
        } else {
            entities = commonCodeJpaRepository.findAllByOrderByGroupCodeAscCodeAsc();
        }
        return CommonCodeListResponse.from(entities);
    }

    @Transactional
    public void create(CommonCodeRequest request) {
        if (commonCodeJpaRepository.existsByGroupCodeAndCode(request.groupCode(), request.code())) {
            throw new IllegalArgumentException("이미 존재하는 코드입니다: " + request.groupCode() + "/" + request.code());
        }
        String now = LocalDateTime.now().format(DT_FMT);
        CommonCodeEntity entity = new CommonCodeEntity();
        entity.setGroupCode(request.groupCode());
        entity.setGroupName(request.groupName());
        entity.setCode(request.code());
        entity.setCodeName(request.codeName());
        entity.setNote(request.note());
        entity.setUseYn(request.useYn() != null ? request.useYn() : "Y");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        commonCodeJpaRepository.save(entity);
    }

    @Transactional
    public void update(Integer id, CommonCodeRequest request) {
        CommonCodeEntity entity = commonCodeJpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("공통코드를 찾을 수 없습니다: " + id));
        if (commonCodeJpaRepository.existsByGroupCodeAndCodeAndIdNot(
                request.groupCode(), request.code(), id)) {
            throw new IllegalArgumentException("이미 존재하는 코드입니다: " + request.groupCode() + "/" + request.code());
        }
        entity.setGroupCode(request.groupCode());
        entity.setGroupName(request.groupName());
        entity.setCode(request.code());
        entity.setCodeName(request.codeName());
        entity.setNote(request.note());
        if (request.useYn() != null) entity.setUseYn(request.useYn());
        entity.setUpdatedAt(LocalDateTime.now().format(DT_FMT));
        commonCodeJpaRepository.save(entity);
    }
}
