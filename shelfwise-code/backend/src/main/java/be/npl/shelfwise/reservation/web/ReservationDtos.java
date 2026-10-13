package be.npl.shelfwise.reservation.web;

import be.npl.shelfwise.reservation.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    public record PlaceReservationRequest(@NotNull Long titleId, @NotNull Long pickupBranchId, Long memberId) {
    }

    public record ReservationView(Long id, Long titleId, String title, Long memberId, String memberName,
                                  Long pickupBranchId, String pickupBranchCode, ReservationStatus status,
                                  Instant placedAt, Integer queuePosition, LocalDate holdUntil, LocalDate notifiedOn,
                                  boolean cancelledByLibrary, java.math.BigDecimal feeCharged) {
    }
}
