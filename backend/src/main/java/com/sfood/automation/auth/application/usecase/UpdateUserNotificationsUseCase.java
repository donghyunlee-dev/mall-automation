package com.sfood.automation.auth.application.usecase;

import com.sfood.automation.auth.application.dto.CurrentUserResponse;
import com.sfood.automation.auth.application.dto.UpdateUserNotificationsRequest;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.UserNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserNotificationsUseCase {

    private final UserAccountRepository userAccountRepository;

    public UpdateUserNotificationsUseCase(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public CurrentUserResponse execute(String email, UpdateUserNotificationsRequest request) {
        var userAccount = userAccountRepository.findByEmail(email)
            .orElseThrow(UserNotFoundException::new);
        userAccount.updateNotifications(request.emailNotificationYn(), request.teamsNotificationYn(), Instant.now());
        return CurrentUserResponse.from(userAccountRepository.save(userAccount));
    }
}
