package com.sfood.automation.auth.domain.port;

import com.sfood.automation.auth.domain.model.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(UserAccount userAccount);

    Optional<UserAccount> findByEmail(String email);

    List<UserAccount> findAllEmailNotificationEnabled();
}
