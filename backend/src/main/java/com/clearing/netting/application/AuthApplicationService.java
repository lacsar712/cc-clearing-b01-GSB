package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.UserAccount;
import com.clearing.netting.domain.model.UserRole;
import com.clearing.netting.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class AuthApplicationService {

    private final UserRepositoryPort userRepository;
    private final TokenService tokenService;

    public AuthApplicationService(UserRepositoryPort userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(String username, String password) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("AUTH_FAILED", "invalid username or password"));
        if (!user.getPasswordHash().equals(hashPassword(password))) {
            throw new DomainException("AUTH_FAILED", "invalid username or password");
        }
        String token = tokenService.issueToken(user.getUsername(), user.getRole().name());
        return new LoginResult(token, user.getUsername(), user.getRole());
    }

    @Transactional
    public UserAccount ensureUser(String username, String rawPassword, UserRole role) {
        return userRepository.findByUsername(username).orElseGet(() ->
                userRepository.save(UserAccount.create(username, hashPassword(rawPassword), role)));
    }

    public static String hashPassword(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record LoginResult(String token, String username, UserRole role) {
    }
}
