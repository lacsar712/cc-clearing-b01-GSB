package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepositoryPort {
    Member save(Member member);

    Optional<Member> findById(String memberId);

    List<Member> findAll();

    List<Member> findByIds(Iterable<String> memberIds);
}
