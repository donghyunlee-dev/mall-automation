package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GodoMemberBusinessJpaRepository extends JpaRepository<GodoMemberBusinessEntity, Long> {

    Optional<GodoMemberBusinessEntity> findByMemNo(Long memNo);

    @Query("SELECT b.memNo FROM GodoMemberBusinessEntity b WHERE b.memNo IN :memNos")
    List<Long> findMemNosByMemNoIn(@Param("memNos") List<Long> memNos);
}
