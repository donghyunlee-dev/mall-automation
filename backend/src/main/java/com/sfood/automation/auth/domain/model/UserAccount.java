package com.sfood.automation.auth.domain.model;

import java.time.Instant;

public class UserAccount {

    private final Integer id;
    private final String email;
    private String emailNotificationYn;
    private String teamsNotificationYn;
    private Instant lastLoginAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserAccount(
        Integer id,
        String email,
        String emailNotificationYn,
        String teamsNotificationYn,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.emailNotificationYn = emailNotificationYn;
        this.teamsNotificationYn = teamsNotificationYn;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserAccount create(String email, Instant now) {
        return new UserAccount(null, email, "N", "N", now, now, now);
    }

    public void markLoggedIn(Instant now) {
        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    public void updateNotifications(String emailNotificationYn, String teamsNotificationYn, Instant now) {
        this.emailNotificationYn = emailNotificationYn;
        this.teamsNotificationYn = teamsNotificationYn;
        this.updatedAt = now;
    }

    public boolean isEmailNotificationEnabled() {
        return "Y".equals(emailNotificationYn);
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailNotificationYn() {
        return emailNotificationYn;
    }

    public String getTeamsNotificationYn() {
        return teamsNotificationYn;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
