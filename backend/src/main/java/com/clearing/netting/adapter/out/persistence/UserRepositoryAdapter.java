package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.UserJpaRepository;
import com.clearing.netting.domain.model.UserAccount;
import com.clearing.netting.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    public UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount user) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(user)));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username).map(PersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }
}
