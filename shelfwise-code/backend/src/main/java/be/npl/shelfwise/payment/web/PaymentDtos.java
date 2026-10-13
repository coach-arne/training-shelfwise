package be.npl.shelfwise.payment.web;

import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.payment.entity.PaymentSessionStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record DeskPaymentRequest(@NotNull BigDecimal amount, @NotNull PaymentMethod method) {
    }

    public record AllocationView(Long chargeId, ChargeType chargeType, BigDecimal amount,
                                 BigDecimal outstandingAfter) {
    }

    /** The receipt of a payment (N-10 is sent as well). */
    public record PaymentReceipt(Long paymentId, Long memberId, String memberName, BigDecimal amount,
                                 PaymentMethod method, LocalDate date, List<AllocationView> allocations,
                                 BigDecimal balanceAfter, boolean blockedAfter) {
    }

    /** Empty {@code chargeIds} means: pay the full balance. */
    public record StartOnlinePaymentRequest(List<Long> chargeIds) {
    }

    public record OnlinePaymentView(String reference, BigDecimal amount, PaymentSessionStatus status,
                                    List<Long> chargeIds, String message) {
    }

    public enum ProviderResult {
        SUCCEEDS, FAILS, CANCELLED
    }

    public record ProviderResultRequest(@NotNull ProviderResult result) {
    }
}
