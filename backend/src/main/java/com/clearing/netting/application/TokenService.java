package com.clearing.netting.application;

public interface TokenService {
    String issueToken(String username, String role);

    TokenPayload parse(String token);

    record TokenPayload(String username, String role) {
    }
}
