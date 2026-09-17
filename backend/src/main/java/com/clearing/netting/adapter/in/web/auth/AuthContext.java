package com.clearing.netting.adapter.in.web.auth;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static AuthUser require() {
        AuthUser user = get();
        if (user == null) {
            throw new com.clearing.netting.domain.exception.DomainException("UNAUTHORIZED", "authentication required");
        }
        return user;
    }

    public static void requireOperator() {
        AuthUser user = require();
        if (!user.isOperator()) {
            throw new com.clearing.netting.domain.exception.DomainException("FORBIDDEN", "operator role required");
        }
    }
}
