package com.sfood.automation.auth.application.dto;

import com.sfood.automation.auth.domain.model.UserAccount;

public record CurrentUserResponse(
    String email,
    String emailNotificationYn,
    String teamsNotificationYn,
    String lastLoginAt,
    String createdAt,
    String updatedAt
) {
    public static CurrentUserResponse from(UserAccount userAccount) {
        return new CurrentUserResponse(
            userAccount.getEmail(),
            userAccount.getEmailNotificationYn(),
            userAccount.getTeamsNotificationYn(),
            userAccount.getLastLoginAt() == null ? null : userAccount.getLastLoginAt().toString(),
            userAccount.getCreatedAt().toString(),
            userAccount.getUpdatedAt().toString()
        );
    }
}
