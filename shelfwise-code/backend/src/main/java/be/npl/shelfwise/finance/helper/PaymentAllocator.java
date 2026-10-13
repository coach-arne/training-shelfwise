package be.npl.shelfwise.finance.helper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** BR-FIN-09: a payment is allocated to the oldest outstanding charge first, then the next oldest, and so on. */
public final class PaymentAllocator {

    public record ChargeOutstanding(Long chargeId, BigDecimal outstanding) {
    }

    public record Allocation(Long chargeId, BigDecimal amount) {
    }

    private PaymentAllocator() {
    }

    /** @param chargesOldestFirst the open charges ordered by charge date; a charge can be partially paid */
    public static List<Allocation> allocateOldestFirst(List<ChargeOutstanding> chargesOldestFirst, BigDecimal amount) {
        List<Allocation> allocations = new ArrayList<>();
        BigDecimal remaining = amount;
        for (ChargeOutstanding charge : chargesOldestFirst) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal part = remaining.min(charge.outstanding());
            if (part.signum() > 0) {
                allocations.add(new Allocation(charge.chargeId(), part));
                remaining = remaining.subtract(part);
            }
        }
        return allocations;
    }
}
