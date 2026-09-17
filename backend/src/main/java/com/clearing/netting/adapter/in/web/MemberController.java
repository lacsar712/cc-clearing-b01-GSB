package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.application.MemberApplicationService;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberApplicationService memberService;

    public MemberController(MemberApplicationService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<MemberResponse> list() {
        AuthContext.require();
        return memberService.listMembers().stream().map(MemberResponse::from).collect(Collectors.toList());
    }

    @PostMapping
    public MemberResponse create(@Valid @RequestBody CreateMemberRequest request) {
        AuthContext.requireOperator();
        return MemberResponse.from(memberService.createMember(request.name()));
    }

    @PostMapping("/{id}/status")
    public MemberResponse updateStatus(@PathVariable("id") String id, @Valid @RequestBody StatusRequest request) {
        AuthContext.requireOperator();
        return MemberResponse.from(memberService.updateStatus(id, request.status()));
    }

    public record CreateMemberRequest(@NotBlank String name) {
    }

    public record StatusRequest(@NotNull MemberStatus status) {
    }

    public record MemberResponse(String memberId, String name, MemberStatus status) {
        static MemberResponse from(Member m) {
            return new MemberResponse(m.getMemberId(), m.getName(), m.getStatus());
        }
    }
}
