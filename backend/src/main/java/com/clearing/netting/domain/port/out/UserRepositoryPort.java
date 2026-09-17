package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.UserAccount;

import java.util.Optional;

public interface UserRepositoryPort {
    UserAccount save(UserAccount user);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
