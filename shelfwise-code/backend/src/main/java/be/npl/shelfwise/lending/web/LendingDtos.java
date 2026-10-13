package be.npl.shelfwise.lending.web;

import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.helper.CopyRouter.RoutingOutcome;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class LendingDtos {

    private LendingDtos() {
    }

    /**
     * A loan as shown to members and staff. The accrued fine (F-FIN-07) is informative only: it is not a charge and
     * does not count towards the account balance.
     */
    public record LoanView(Long id, Long copyId, String barcode, Long titleId, String title,
                           MaterialType materialType, Long memberId, String memberName, LocalDate checkoutDate,
                           LocalDate dueDate, LocalDate returnDate, LoanStatus status, int renewalsUsed,
                           int renewalsLeft, boolean overdue, int chargeableDays, BigDecimal accruedFine,
                           String checkoutBranchCode, boolean canRenew, String renewalRefusal) {
    }

    public record CheckoutRequest(@NotBlank String cardNumber, @NotBlank String barcode) {
    }

    /** The receipt of a checkout lists all open loans and their due dates (F-LEN-01). */
    public record CheckoutReceipt(LoanView loan, String memberName, String cardNumber, LocalDate date,
                                  List<LoanView> openLoans) {
    }

    public record BarcodeRequest(@NotBlank String barcode) {
    }

    public record RoutingInfo(RoutingOutcome outcome, CopyStatus copyStatus, String destinationBranchCode) {
    }

    public record ReturnReceipt(String barcode, String title, LocalDate returnDate, String memberName,
                                BigDecimal fineCharged, int chargeableDays, RoutingInfo routing, boolean lostCopy,
                                boolean replacementCancelled, String message) {
    }

    public record ArrivalReceipt(String barcode, String title, RoutingInfo routing, String message) {
    }
}
