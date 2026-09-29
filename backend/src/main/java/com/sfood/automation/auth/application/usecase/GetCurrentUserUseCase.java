package com.sfood.automation.auth.application.usecase;

import com.sfood.automation.auth.application.dto.CurrentUserResponse;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserUseCase {

    private final UserAccountRepository userAccountRepository;

    public GetCurrentUserUseCase(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse execute(String email) {
        return userAccountRepository.findByEmail(email)
            .map(CurrentUserResponse::from)
            .orElseThrow(UserNotFoundException::new);
    }
}
