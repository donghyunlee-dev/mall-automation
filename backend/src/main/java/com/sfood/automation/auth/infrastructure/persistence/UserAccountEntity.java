package com.sfood.automation.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_notification_yn", nullable = false)
    private String emailNotificationYn;

    @Column(name = "teams_notification_yn", nullable = false)
    private String teamsNotificationYn;

    @Column(name = "last_login_at", nullable = false)
    private String lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailNotificationYn() {
        return emailNotificationYn;
    }

    public void setEmailNotificationYn(String emailNotificationYn) {
        this.emailNotificationYn = emailNotificationYn;
    }

    public String getTeamsNotificationYn() {
        return teamsNotificationYn;
    }

    public void setTeamsNotificationYn(String teamsNotificationYn) {
        this.teamsNotificationYn = teamsNotificationYn;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
