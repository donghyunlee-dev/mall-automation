package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GodoMemberBusinessLogJpaRepository
    extends JpaRepository<GodoMemberBusinessLogEntity, Integer> {

    @Query("SELECT l FROM GodoMemberBusinessLogEntity l WHERE l.memNo = :memNo ORDER BY l.loggedAt DESC, l.id DESC")
    List<GodoMemberBusinessLogEntity> findByMemNoOrderByLoggedAtDesc(@Param("memNo") Long memNo);
}
