package com.sfood.automation.common_code.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonCodeJpaRepository extends JpaRepository<CommonCodeEntity, Integer> {

    List<CommonCodeEntity> findByGroupCodeOrderByCodeAsc(String groupCode);

    List<CommonCodeEntity> findAllByOrderByGroupCodeAscCodeAsc();

    boolean existsByGroupCodeAndCodeAndIdNot(String groupCode, String code, Integer id);

    boolean existsByGroupCodeAndCode(String groupCode, String code);

    List<CommonCodeEntity> findByGroupCodeAndUseYnOrderByCodeAsc(String groupCode, String useYn);

    List<CommonCodeEntity> findByUseYn(String useYn);
}
