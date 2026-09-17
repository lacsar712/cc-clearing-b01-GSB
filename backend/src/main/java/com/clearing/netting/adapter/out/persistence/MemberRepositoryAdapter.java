package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.MemberJpaRepository;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class MemberRepositoryAdapter implements MemberRepositoryPort {

    private final MemberJpaRepository repository;

    public MemberRepositoryAdapter(MemberJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Member save(Member member) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(member)));
    }

    @Override
    public Optional<Member> findById(String memberId) {
        return repository.findById(memberId).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Member> findAll() {
        return repository.findAll().stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Member> findByIds(Iterable<String> memberIds) {
        List<String> ids = StreamSupport.stream(memberIds.spliterator(), false).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return repository.findAllById(ids).stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }
}
