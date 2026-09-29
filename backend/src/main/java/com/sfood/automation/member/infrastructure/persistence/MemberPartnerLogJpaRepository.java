package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPartnerLogJpaRepository
    extends JpaRepository<MemberPartnerLogEntity, Integer> {

    @Query("SELECT l FROM MemberPartnerLogEntity l WHERE l.memNo = :memNo ORDER BY l.loggedAt DESC, l.id DESC")
    List<MemberPartnerLogEntity> findByMemNoOrderByLoggedAtDesc(@Param("memNo") Long memNo);
}
