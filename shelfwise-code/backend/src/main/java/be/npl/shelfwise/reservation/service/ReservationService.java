package be.npl.shelfwise.reservation.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.catalogue.repository.TitleRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.common.TimeHelper;
import be.npl.shelfwise.config.AppProperties;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.service.ChargeService;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.lending.service.CopyRoutingService;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.notification.entity.NotificationType;
import be.npl.shelfwise.notification.service.NotificationDraft;
import be.npl.shelfwise.reservation.entity.Reservation;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import be.npl.shelfwise.reservation.web.ReservationDtos.ReservationView;
import be.npl.shelfwise.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reservations on titles: placing, fee, queue, holds, expiry and cancellation (F-RES-01 to F-RES-05). */
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private static final Set<ReservationStatus> ACTIVE = Set.of(ReservationStatus.WAITING, ReservationStatus.ON_HOLD);

    private final ReservationRepository reservations;
    private final MemberRepository members;
    private final TitleRepository titles;
    private final BranchRepository branches;
    private final CopyRepository copies;
    private final LoanRepository loans;
    private final ChargeRepository charges;
    private final ChargeService chargeService;
    private final MemberStatusService memberStatus;
    private final CopyRoutingService routing;
    private final BusinessDateService businessDate;
    private final AppProperties properties;
    private final RulesProperties rules;

    // ----- placing -----

    /** F-RES-01: reserve a title for a member. The checks follow section 9.1. */
    public ReservationView place(Long memberId, Long titleId, Long pickupBranchId) {
        LocalDate today = businessDate.today();
        Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
        Title title = titles.findById(titleId).orElseThrow(() -> new NotFoundException("Title", titleId));
        Branch pickup = branches.findById(pickupBranchId).orElseThrow(() -> new NotFoundException("Branch", pickupBranchId));

        memberStatus.assertMayUseLending(member, today);
        if (reservations.countByMemberIdAndStatusIn(memberId, ACTIVE) >= rules.maxActiveReservations()) {
            throw new BusinessRuleException("RESERVATION_LIMIT_REACHED", "BR-RES-04",
                    "A member can have at most " + rules.maxActiveReservations() + " active reservations.");
        }
        if (reservations.existsByMemberIdAndTitleIdAndStatusIn(memberId, titleId, ACTIVE)) {
            throw new BusinessRuleException("DUPLICATE_RESERVATION", "BR-RES-05",
                    "The member already has an active reservation for this title.");
        }
        if (loans.memberHasOpenLoanOfTitle(memberId, titleId)) {
            throw new BusinessRuleException("TITLE_ON_LOAN_TO_MEMBER", "BR-RES-05",
                    "The member currently has a copy of this title on loan.");
        }
        if (copies.countByTitleIdAndStatusNotIn(titleId, Set.of(CopyStatus.WITHDRAWN, CopyStatus.LOST)) == 0) {
            throw new BusinessRuleException("NO_COPIES_IN_CIRCULATION", "BR-RES-01",
                    "This title has no copies in circulation.");
        }
        if (copies.existsByTitleIdAndCurrentBranchIdAndStatus(titleId, pickupBranchId, CopyStatus.AVAILABLE)) {
            throw new BusinessRuleException("COPY_AVAILABLE_AT_BRANCH", "BR-RES-01",
                    "A copy is available at " + pickup.getName() + ": the member can borrow it directly.");
        }

        Reservation reservation = new Reservation();
        reservation.setTitle(title);
        reservation.setMember(member);
        reservation.setPickupBranch(pickup);
        reservation.setStatus(ReservationStatus.WAITING);
        reservation.setPlacedAt(TimeHelper.onBusinessDate(today, ZoneId.of(properties.timeZone())));
        reservation.setReminderSent(false);
        reservation.setCancelledByLibrary(false);
        reservations.save(reservation);

        BigDecimal fee = member.getCategory().reservationFee();
        if (fee.signum() > 0) {
            chargeService.create(member, ChargeType.RESERVATION_FEE, fee, today, null, reservation,
                    "Reservation fee for \"" + title.getTitle() + "\"");
        }
        return toView(reservation);
    }

    // ----- cancelling -----

    /** F-RES-05: the member (portal) or a librarian (desk) cancels. The fee is not refunded (BR-RES-03). */
    public ReservationView cancel(Long reservationId, CurrentUser actor) {
        Reservation reservation = find(reservationId);
        if (actor.isMember() && !reservation.getMember().getId().equals(actor.accountId())) {
            throw new AccessDeniedException("Not your reservation");
        }
        ensureActive(reservation);
        end(reservation, ReservationStatus.CANCELLED, businessDate.today(), false);
        return toView(reservation);
    }

    /**
     * The library cancels (section 7.3): the fee charge is cancelled and anything already paid on it is refunded.
     */
    public void cancelByLibrary(Reservation reservation, LocalDate today) {
        end(reservation, ReservationStatus.CANCELLED, today, true);
        charges.findFirstByReservationIdAndTypeOrderByIdDesc(reservation.getId(), ChargeType.RESERVATION_FEE)
                .ifPresent((Charge fee) -> chargeService.cancel(fee, today, "Reservation cancelled by the library"));
    }

    /** When the last copy of a title is withdrawn, all its active reservations are cancelled (AC-RES-10). */
    public int cancelAllActiveForTitle(Long titleId, LocalDate today) {
        List<Reservation> active = reservations.findByTitleIdAndStatusIn(titleId, ACTIVE);
        active.forEach(r -> cancelByLibrary(r, today));
        return active.size();
    }

    // ----- daily processing -----

    /**
     * F-RES-04: holds whose last hold day has been reached expire. N-08 is added to {@code drafts}; the copy goes to
     * the next waiting reservation or becomes AVAILABLE.
     */
    public int expireHolds(LocalDate today, List<NotificationDraft> drafts) {
        List<Reservation> due = reservations.findByStatusAndHoldUntilLessThanEqualOrderByIdAsc(ReservationStatus.ON_HOLD, today);
        for (Reservation reservation : due) {
            Copy copy = reservation.getAssignedCopy();
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setEndedOn(today);
            reservations.save(reservation);
            drafts.add(new NotificationDraft(reservation.getMember(), NotificationType.N_08, List.of(
                    "Your reservation for \"" + reservation.getTitle().getTitle()
                            + "\" has expired because the copy was not collected.")));
            if (copy != null) {
                routing.offerToQueue(copy, today);
            }
        }
        return due.size();
    }

    /** N-07: 5 days after the pickup notification, for holds that are still waiting to be collected. */
    public int pickupReminders(LocalDate today, List<NotificationDraft> drafts) {
        LocalDate notifiedOn = today.minusDays(rules.pickupReminderDays());
        List<Reservation> due = reservations.findByStatusAndNotifiedOnAndReminderSentFalse(ReservationStatus.ON_HOLD, notifiedOn);
        for (Reservation reservation : due) {
            reservation.setReminderSent(true);
            reservations.save(reservation);
            drafts.add(new NotificationDraft(reservation.getMember(), NotificationType.N_07, List.of(
                    "Reminder: \"" + reservation.getTitle().getTitle() + "\" is waiting for you at "
                            + reservation.getPickupBranch().getName() + " until " + reservation.getHoldUntil() + ".")));
        }
        return due.size();
    }

    // ----- views -----

    @Transactional(readOnly = true)
    public List<ReservationView> listForMember(Long memberId) {
        return reservations.findByMemberIdOrderByPlacedAtDescIdDesc(memberId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ReservationView toView(Reservation r) {
        Integer position = null;
        if (r.getStatus() == ReservationStatus.WAITING && r.getAssignedCopy() == null) {
            List<Reservation> queue = reservations.findByTitleIdAndStatusAndAssignedCopyIsNullOrderByPlacedAtAscIdAsc(
                    r.getTitle().getId(), ReservationStatus.WAITING);
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getId().equals(r.getId())) {
                    position = i + 1;
                }
            }
        }
        BigDecimal fee = charges.findFirstByReservationIdAndTypeOrderByIdDesc(r.getId(), ChargeType.RESERVATION_FEE)
                .map(Charge::getOriginalAmount).orElse(null);
        return new ReservationView(r.getId(), r.getTitle().getId(), r.getTitle().getTitle(), r.getMember().getId(),
                r.getMember().fullName(), r.getPickupBranch().getId(), r.getPickupBranch().getCode(), r.getStatus(),
                r.getPlacedAt(), position, r.getHoldUntil(), r.getNotifiedOn(), r.isCancelledByLibrary(), fee);
    }

    // ----- helpers -----

    private Reservation find(Long id) {
        return reservations.findById(id).orElseThrow(() -> new NotFoundException("Reservation", id));
    }

    private void ensureActive(Reservation reservation) {
        if (!reservation.getStatus().isActive()) {
            throw new BusinessRuleException("RESERVATION_NOT_ACTIVE", "F-RES-05",
                    "Only a WAITING or ON_HOLD reservation can be cancelled.");
        }
    }

    /** Ends an active reservation. A copy on hold goes to the next reservation or becomes AVAILABLE (section 9.5). */
    private void end(Reservation reservation, ReservationStatus status, LocalDate today, boolean byLibrary) {
        ensureActive(reservation);
        Copy copy = reservation.getAssignedCopy();
        boolean wasOnHold = reservation.getStatus() == ReservationStatus.ON_HOLD;
        reservation.setStatus(status);
        reservation.setEndedOn(today);
        reservation.setCancelledByLibrary(byLibrary);
        if (copy != null && !wasOnHold) {
            // A copy that is still in transit keeps travelling; it is routed again when it arrives.
            reservation.setAssignedCopy(null);
        }
        reservations.save(reservation);
        if (copy != null && wasOnHold) {
            routing.offerToQueue(copy, today);
        }
    }
}
