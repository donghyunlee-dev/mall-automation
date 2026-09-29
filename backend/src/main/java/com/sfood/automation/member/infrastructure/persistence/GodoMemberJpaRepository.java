package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GodoMemberJpaRepository extends JpaRepository<GodoMemberEntity, Long> {

    Optional<GodoMemberEntity> findByMemNo(Long memNo);

    @Query("SELECT m.memNo FROM GodoMemberEntity m WHERE m.memNo IN :memNos")
    List<Long> findMemNosByMemNoIn(@Param("memNos") List<Long> memNos);

    @Query("SELECT m.memNo FROM GodoMemberEntity m WHERE m.deletedFl = 'n'")
    List<Long> findAllActiveMemNos();

    @Modifying
    @Query("UPDATE GodoMemberEntity m SET m.deletedFl = 'y', m.updatedAt = :updatedAt WHERE m.memNo IN :memNos")
    int softDeleteByMemNos(@Param("memNos") List<Long> memNos, @Param("updatedAt") String updatedAt);

    @Modifying
    @Query("DELETE FROM GodoMemberEntity m WHERE m.memNo = :memNo")
    void hardDeleteByMemNo(@Param("memNo") Long memNo);
}
