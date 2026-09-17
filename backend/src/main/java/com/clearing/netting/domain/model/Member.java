package com.clearing.netting.domain.model;

import java.util.Objects;
import java.util.UUID;

public class Member {
    private final String memberId;
    private String name;
    private MemberStatus status;

    public Member(String memberId, String name, MemberStatus status) {
        this.memberId = Objects.requireNonNull(memberId);
        this.name = Objects.requireNonNull(name);
        this.status = Objects.requireNonNull(status);
    }

    public static Member create(String name) {
        return new Member(UUID.randomUUID().toString(), name, MemberStatus.ACTIVE);
    }

    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public MemberStatus getStatus() {
        return status;
    }
}
