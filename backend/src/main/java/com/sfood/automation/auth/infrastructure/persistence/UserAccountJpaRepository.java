package com.sfood.automation.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Integer> {

    Optional<UserAccountEntity> findByEmail(String email);

    List<UserAccountEntity> findAllByEmailNotificationYnOrderByEmailAsc(String emailNotificationYn);
}
