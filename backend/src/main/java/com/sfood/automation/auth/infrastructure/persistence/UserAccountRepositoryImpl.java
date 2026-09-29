package com.sfood.automation.auth.infrastructure.persistence;

import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final UserAccountJpaRepository userAccountJpaRepository;

    public UserAccountRepositoryImpl(UserAccountJpaRepository userAccountJpaRepository) {
        this.userAccountJpaRepository = userAccountJpaRepository;
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        return toDomain(userAccountJpaRepository.save(toEntity(userAccount)));
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return userAccountJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<UserAccount> findAllEmailNotificationEnabled() {
        return userAccountJpaRepository.findAllByEmailNotificationYnOrderByEmailAsc("Y").stream()
            .map(this::toDomain)
            .toList();
    }

    private UserAccountEntity toEntity(UserAccount userAccount) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setId(userAccount.getId());
        entity.setEmail(userAccount.getEmail());
        entity.setEmailNotificationYn(userAccount.getEmailNotificationYn());
        entity.setTeamsNotificationYn(userAccount.getTeamsNotificationYn());
        entity.setLastLoginAt(userAccount.getLastLoginAt().toString());
        entity.setCreatedAt(userAccount.getCreatedAt().toString());
        entity.setUpdatedAt(userAccount.getUpdatedAt().toString());
        return entity;
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
            entity.getId(),
            entity.getEmail(),
            entity.getEmailNotificationYn(),
            entity.getTeamsNotificationYn(),
            SqliteInstantParser.parse(entity.getLastLoginAt()),
            SqliteInstantParser.parse(entity.getCreatedAt()),
            SqliteInstantParser.parse(entity.getUpdatedAt())
        );
    }
}
