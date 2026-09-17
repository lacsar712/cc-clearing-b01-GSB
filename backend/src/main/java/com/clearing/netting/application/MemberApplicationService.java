package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberApplicationService {

    private final MemberRepositoryPort memberRepository;

    public MemberApplicationService(MemberRepositoryPort memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<Member> listMembers() {
        return memberRepository.findAll();
    }

    @Transactional
    public Member createMember(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("INVALID_NAME", "member name is required");
        }
        return memberRepository.save(Member.create(name.trim()));
    }

    @Transactional
    public Member updateStatus(String memberId, MemberStatus status) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new DomainException("MEMBER_NOT_FOUND", "member not found: " + memberId));
        if (status == MemberStatus.ACTIVE) {
            member.activate();
        } else if (status == MemberStatus.SUSPENDED) {
            member.suspend();
        } else {
            throw new DomainException("INVALID_STATUS", "unknown status");
        }
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member getMember(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new DomainException("MEMBER_NOT_FOUND", "member not found: " + memberId));
    }
}
