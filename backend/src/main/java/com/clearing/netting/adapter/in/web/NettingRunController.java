package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.application.NettingApplicationService;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/netting-runs")
public class NettingRunController {

    private final NettingApplicationService nettingService;

    public NettingRunController(NettingApplicationService nettingService) {
        this.nettingService = nettingService;
    }

    @GetMapping
    public List<RunResponse> list() {
        AuthContext.require();
        return nettingService.listRuns().stream().map(RunResponse::from).collect(Collectors.toList());
    }

    @PostMapping
    public ExecuteResponse execute(@Valid @RequestBody ExecuteRequest request) {
        AuthContext.requireOperator();
        NettingApplicationService.NettingRunResult result =
                nettingService.execute(request.settleDate(), request.currency());
        return new ExecuteResponse(
                RunResponse.from(result.run()),
                result.positions().stream().map(PositionResponse::from).collect(Collectors.toList()),
                sumNet(result.positions()));
    }

    @GetMapping("/{id}")
    public RunDetailResponse get(@PathVariable("id") String id) {
        AuthContext.require();
        NettingRun run = nettingService.getRun(id);
        List<TradeObligation> obligations = nettingService.getRunObligations(id);
        List<NetPosition> positions = nettingService.getPositions(id);
        return new RunDetailResponse(
                RunResponse.from(run),
                obligations.stream().map(ObligationBrief::from).collect(Collectors.toList()),
                positions.stream().map(PositionResponse::from).collect(Collectors.toList()),
                sumNet(positions));
    }

    @GetMapping("/{id}/positions")
    public List<PositionResponse> positions(@PathVariable("id") String id) {
        AuthContext.require();
        return nettingService.getPositions(id).stream().map(PositionResponse::from).collect(Collectors.toList());
    }

    @PostMapping("/{id}/settle")
    public RunResponse settle(@PathVariable("id") String id) {
        AuthContext.requireOperator();
        return RunResponse.from(nettingService.settle(id));
    }

    private BigDecimal sumNet(List<NetPosition> positions) {
        return positions.stream()
                .map(NetPosition::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record ExecuteRequest(@NotNull LocalDate settleDate, @NotBlank String currency) {
    }

    public record RunResponse(
            String runId,
            LocalDate settleDate,
            String currency,
            NettingRunStatus status,
            Instant createdAt,
            String failureReason) {
        static RunResponse from(NettingRun r) {
            return new RunResponse(
                    r.getRunId(),
                    r.getSettleDate(),
                    r.getCurrency(),
                    r.getStatus(),
                    r.getCreatedAt(),
                    r.getFailureReason());
        }
    }

    public record PositionResponse(
            String positionId,
            String runId,
            String memberId,
            String currency,
            BigDecimal netAmount) {
        static PositionResponse from(NetPosition p) {
            return new PositionResponse(
                    p.getPositionId(),
                    p.getRunId(),
                    p.getMemberId(),
                    p.getCurrency(),
                    p.getNetAmount());
        }
    }

    public record ObligationBrief(
            String obligationId,
            String payerMemberId,
            String payeeMemberId,
            String currency,
            BigDecimal amount,
            ObligationStatus status) {
        static ObligationBrief from(TradeObligation o) {
            return new ObligationBrief(
                    o.getObligationId(),
                    o.getPayerMemberId(),
                    o.getPayeeMemberId(),
                    o.getCurrency(),
                    o.getAmount(),
                    o.getStatus());
        }
    }

    public record ExecuteResponse(RunResponse run, List<PositionResponse> positions, BigDecimal sumNetAmount) {
    }

    public record RunDetailResponse(
            RunResponse run,
            List<ObligationBrief> obligations,
            List<PositionResponse> positions,
            BigDecimal sumNetAmount) {
    }
}
