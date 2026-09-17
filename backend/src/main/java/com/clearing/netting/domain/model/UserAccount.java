package com.clearing.netting.domain.model;

import java.util.Objects;
import java.util.UUID;

public class UserAccount {
    private final String userId;
    private final String username;
    private final String passwordHash;
    private final UserRole role;

    public UserAccount(String userId, String username, String passwordHash, UserRole role) {
        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
    }

    public static UserAccount create(String username, String passwordHash, UserRole role) {
        return new UserAccount(UUID.randomUUID().toString(), username, passwordHash, role);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }
}
