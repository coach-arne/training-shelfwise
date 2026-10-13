package be.npl.shelfwise.lending.service;

import be.npl.shelfwise.calendar.service.CalendarService;
import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.lending.helper.CopyRouter;
import be.npl.shelfwise.lending.helper.CopyRouter.RoutingOutcome;
import be.npl.shelfwise.notification.entity.NotificationType;
import be.npl.shelfwise.notification.service.NotificationService;
import be.npl.shelfwise.reservation.entity.Reservation;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.helper.HoldPeriodCalculator;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides where a copy goes when it becomes available (BR-RES-07, F-LEN-06): on hold for the first waiting
 * reservation, in transit to its pickup branch or to the home branch, or available.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CopyRoutingService {

    private final ReservationRepository reservations;
    private final CopyRepository copies;
    private final CalendarService calendar;
    private final NotificationService notifications;
    private final RulesProperties rules;

    public record RoutingResult(RoutingOutcome outcome, CopyStatus copyStatus, String destinationBranchCode,
                                Long reservationId) {
    }

    /**
     * Applies the routing table to a copy that is checked in at {@code atBranch}, or that simply became available
     * where it is. The current branch of the copy is updated.
     *
     * @param returnToHomeBranch true for returns and arrivals; false when the copy just stays where it is
     */
    public RoutingResult route(Copy copy, Branch atBranch, LocalDate today, boolean returnToHomeBranch) {
        copy.setCurrentBranch(atBranch);
        copy.setTransitDestination(null);
        Optional<Reservation> next = firstWaitingWithoutCopy(copy.getTitle().getId());
        RoutingOutcome outcome = CopyRouter.decide(atBranch.getId(), copy.getHomeBranch().getId(),
                next.map(r -> r.getPickupBranch().getId()).orElse(null), returnToHomeBranch);
        RoutingResult result = switch (outcome) {
            case HOLD_HERE -> {
                putOnHold(copy, next.orElseThrow(), today);
                yield new RoutingResult(outcome, CopyStatus.ON_HOLD, null, next.get().getId());
            }
            case TRANSIT_TO_PICKUP -> {
                Reservation reservation = next.orElseThrow();
                reservation.setAssignedCopy(copy);
                reservations.save(reservation);
                copy.setStatus(CopyStatus.IN_TRANSIT);
                copy.setTransitDestination(reservation.getPickupBranch());
                yield new RoutingResult(outcome, CopyStatus.IN_TRANSIT, reservation.getPickupBranch().getCode(),
                        reservation.getId());
            }
            case AVAILABLE_HERE -> {
                copy.setStatus(CopyStatus.AVAILABLE);
                yield new RoutingResult(outcome, CopyStatus.AVAILABLE, null, null);
            }
            case TRANSIT_TO_HOME -> {
                copy.setStatus(CopyStatus.IN_TRANSIT);
                copy.setTransitDestination(copy.getHomeBranch());
                yield new RoutingResult(outcome, CopyStatus.IN_TRANSIT, copy.getHomeBranch().getCode(), null);
            }
        };
        copies.save(copy);
        return result;
    }

    /**
     * Check-in of a copy that arrives at a branch after a transfer. A copy assigned to a reservation that is still
     * WAITING becomes ON_HOLD for it; otherwise the routing table is applied again (section 8.6).
     */
    public RoutingResult arrive(Copy copy, Branch atBranch, LocalDate today) {
        if (copy.getStatus() != CopyStatus.IN_TRANSIT) {
            throw new BusinessRuleException("NOT_IN_TRANSIT", "F-LEN-06",
                    "Copy " + copy.getBarcode() + " is not in transit (status " + copy.getStatus() + ").");
        }
        Branch destination = copy.getTransitDestination();
        if (destination != null && !destination.getId().equals(atBranch.getId())) {
            throw new BusinessRuleException("WRONG_BRANCH", "F-LEN-06",
                    "Copy " + copy.getBarcode() + " is in transit to " + destination.getName() + ".");
        }
        Optional<Reservation> assigned = reservations.findFirstByAssignedCopyIdAndStatusIn(copy.getId(),
                List.of(ReservationStatus.WAITING));
        if (assigned.isPresent()) {
            copy.setCurrentBranch(atBranch);
            copy.setTransitDestination(null);
            putOnHold(copy, assigned.get(), today);
            copies.save(copy);
            return new RoutingResult(RoutingOutcome.HOLD_HERE, CopyStatus.ON_HOLD, null, assigned.get().getId());
        }
        return route(copy, atBranch, today, true);
    }

    /** BR-RES-07: a copy that became available (repair, new copy, released hold) is first offered to the queue. */
    public RoutingResult offerToQueue(Copy copy, LocalDate today) {
        return route(copy, copy.getCurrentBranch(), today, false);
    }

    public Optional<Reservation> firstWaitingWithoutCopy(Long titleId) {
        return reservations.findByTitleIdAndStatusAndAssignedCopyIsNullOrderByPlacedAtAscIdAsc(
                titleId, ReservationStatus.WAITING).stream().findFirst();
    }

    private void putOnHold(Copy copy, Reservation reservation, LocalDate today) {
        copy.setStatus(CopyStatus.ON_HOLD);
        reservation.setStatus(ReservationStatus.ON_HOLD);
        reservation.setAssignedCopy(copy);
        reservation.setNotifiedOn(today);
        reservation.setHoldUntil(HoldPeriodCalculator.lastHoldDay(today, rules.holdPeriodDays(),
                calendar.closingDays(today, today.plusDays(rules.holdPeriodDays() + 31L))));
        reservation.setReminderSent(false);
        reservations.save(reservation);
        notifications.sendNow(NotificationType.N_06, reservation.getMember(), today, List.of(
                "\"" + copy.getTitle().getTitle() + "\" is ready for pickup at " + copy.getCurrentBranch().getName()
                        + ". Last pickup day: " + reservation.getHoldUntil() + "."));
    }
}
