package com.clearing.netting.adapter.in.web.auth;

public record AuthUser(String username, String role) {
    public boolean isOperator() {
        return "OPERATOR".equalsIgnoreCase(role);
    }
}
