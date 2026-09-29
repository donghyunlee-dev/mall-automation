package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPartnerJpaRepository extends JpaRepository<MemberPartnerEntity, Long> {

    Optional<MemberPartnerEntity> findByMemNo(Long memNo);

    @Query("SELECT p.memNo FROM MemberPartnerEntity p WHERE p.memNo IN :memNos")
    List<Long> findMemNosByMemNoIn(@Param("memNos") List<Long> memNos);
}
