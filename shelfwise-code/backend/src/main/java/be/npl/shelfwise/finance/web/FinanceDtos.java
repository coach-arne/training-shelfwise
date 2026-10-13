package be.npl.shelfwise.finance.web;

import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.entity.TransactionType;
import be.npl.shelfwise.member.service.MemberStatusService.BlockReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FinanceDtos {

    private FinanceDtos() {
    }

    public record TransactionView(TransactionType type, BigDecimal amount, LocalDate date, String note) {
    }

    /** A charge with its history of payments, waivers, refunds and cancellations (section 11.3). */
    public record ChargeView(Long id, ChargeType type, BigDecimal originalAmount, BigDecimal outstandingAmount,
                             LocalDate chargeDate, ChargeStatus status, String description, Long loanId,
                             Long reservationId, List<TransactionView> history) {
    }

    public record AccountView(Long memberId, BigDecimal balance, boolean blocked,
                              List<BlockReason> blockReasons, List<ChargeView> charges) {
    }

    public record WaiveRequest(@NotNull BigDecimal amount, @NotBlank String reason) {
    }
}
