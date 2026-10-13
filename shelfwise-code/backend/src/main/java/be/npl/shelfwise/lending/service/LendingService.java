package be.npl.shelfwise.lending.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.calendar.service.CalendarService;
import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.repository.BranchRepository;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.finance.service.ChargeService;
import be.npl.shelfwise.finance.service.FineService;
import be.npl.shelfwise.lending.entity.Channel;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanEventType;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.helper.DueDateCalculator;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.lending.service.CopyRoutingService.RoutingResult;
import be.npl.shelfwise.lending.web.LendingDtos.ArrivalReceipt;
import be.npl.shelfwise.lending.web.LendingDtos.CheckoutReceipt;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import be.npl.shelfwise.lending.web.LendingDtos.ReturnReceipt;
import be.npl.shelfwise.lending.web.LendingDtos.RoutingInfo;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.reservation.entity.Reservation;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import be.npl.shelfwise.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checkout, renewal, return and transfers (F-LEN-01 to F-LEN-07). */
@Service
@RequiredArgsConstructor
@Transactional
public class LendingService {

    private final MemberRepository members;
    private final CopyRepository copies;
    private final LoanRepository loans;
    private final BranchRepository branches;
    private final ReservationRepository reservations;
    private final MemberStatusService memberStatus;
    private final RenewalPolicy renewalPolicy;
    private final CalendarService calendar;
    private final BusinessDateService businessDate;
    private final FineService fines;
    private final ChargeService chargeService;
    private final CopyRoutingService routing;
    private final LostItemService lostItems;
    private final LoanEventService loanEvents;
    private final LoanQueryService loanQueries;

    // ----- checkout -----

    /**
     * F-LEN-01: the checks are made in the order of the specification and cannot be overridden by any role
     * (BR-LOA-07): membership valid, not blocked, loan limit, copy available (or on hold for this member).
     */
    public CheckoutReceipt checkout(String cardNumber, String barcode, CurrentUser staff) {
        LocalDate today = businessDate.today();
        Branch branch = branchOf(staff);
        Member member = members.findByCardNumber(cardNumber.trim())
                .orElseThrow(() -> new NotFoundException("Member with card", cardNumber));
        Copy copy = copies.findByBarcode(barcode.trim()).orElseThrow(() -> new NotFoundException("Copy", barcode));

        memberStatus.assertMembershipValid(member, today);
        memberStatus.assertNotBlockedByBalance(member);
        memberStatus.assertBelowLoanLimit(member);
        Optional<Reservation> hold = holdFor(copy, member);
        if (copy.getStatus() != CopyStatus.AVAILABLE && hold.isEmpty()) {
            throw new BusinessRuleException("COPY_NOT_AVAILABLE", "F-LEN-01",
                    "Copy " + copy.getBarcode() + " cannot be borrowed: its status is " + copy.getStatus() + ".");
        }

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setMember(member);
        loan.setMemberCategory(member.getCategory());
        loan.setCheckoutDate(today);
        loan.setDueDate(DueDateCalculator.dueDate(today, copy.getTitle().getMaterialType(),
                calendar.closingDays(today, today.plusDays(copy.getTitle().getMaterialType().loanPeriodDays() + 31L))));
        loan.setStatus(LoanStatus.OPEN);
        loan.setRenewalCount(0);
        loan.setCheckoutBranch(branch);
        loans.save(loan);

        copy.setStatus(CopyStatus.ON_LOAN);
        copies.save(copy);
        hold.ifPresent(reservation -> {
            reservation.setStatus(ReservationStatus.COLLECTED);
            reservation.setEndedOn(today);
            reservations.save(reservation);
        });
        loanEvents.record(loan, LoanEventType.CHECKOUT, today, branch, Channel.DESK);
        return new CheckoutReceipt(loanQueries.toView(loan, today), member.fullName(), member.getCardNumber(), today,
                loanQueries.openLoansOf(member.getId()));
    }

    private Optional<Reservation> holdFor(Copy copy, Member member) {
        if (copy.getStatus() != CopyStatus.ON_HOLD) {
            return Optional.empty();
        }
        return reservations.findFirstByAssignedCopyIdAndStatusIn(copy.getId(), List.of(ReservationStatus.ON_HOLD))
                .filter(r -> r.getMember().getId().equals(member.getId()));
    }

    // ----- renewal -----

    /**
     * F-LEN-04: renewal by a member in the portal or by a librarian at the desk, with the same rules. Members can
     * only renew their own loans.
     */
    public LoanView renew(Long loanId, CurrentUser user) {
        LocalDate today = businessDate.today();
        Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
        if (user.isMember() && (loan.getMember() == null || !loan.getMember().getId().equals(user.accountId()))) {
            throw new AccessDeniedException("Not your loan");
        }
        Optional<BusinessRuleException> refusal = renewalPolicy.refusal(loan, today);
        if (refusal.isPresent()) {
            throw refusal.get();
        }
        var material = loan.getCopy().getTitle().getMaterialType();
        loan.setDueDate(DueDateCalculator.dueDate(loan.getDueDate(), material,
                calendar.closingDays(loan.getDueDate(), loan.getDueDate().plusDays(material.loanPeriodDays() + 31L))));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loans.save(loan);
        if (user.isMember()) {
            loanEvents.record(loan, LoanEventType.RENEWAL, today, null, Channel.ONLINE);
        } else {
            loanEvents.record(loan, LoanEventType.RENEWAL, today, branchOf(user), Channel.DESK);
        }
        return loanQueries.toView(loan, today);
    }

    // ----- return -----

    /** F-LEN-05: a copy can be returned at any branch (BR-LOA-06). Lost copies are handled by section 10.9. */
    public ReturnReceipt returnCopy(String barcode, CurrentUser staff) {
        LocalDate today = businessDate.today();
        Branch branch = branchOf(staff);
        Copy copy = copies.findByBarcode(barcode.trim()).orElseThrow(() -> new NotFoundException("Copy", barcode));

        if (copy.getStatus() == CopyStatus.LOST) {
            return returnLostCopy(copy, branch, today);
        }
        Loan loan = loans.findFirstByCopyIdAndStatusOrderByIdDesc(copy.getId(), LoanStatus.OPEN)
                .filter(l -> copy.getStatus() == CopyStatus.ON_LOAN)
                .orElseThrow(() -> new BusinessRuleException("COPY_NOT_ON_LOAN", "F-LEN-05",
                        "Copy " + copy.getBarcode() + " is not on loan (status " + copy.getStatus() + ")."));

        // 1. close the loan with the return date
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(today);
        loan.setReturnBranch(branch);
        loans.save(loan);
        loanEvents.record(loan, LoanEventType.RETURN, today, branch, Channel.DESK);

        // 2. overdue fine, if more than 0.00
        FineResult fine = fines.fineFor(loan, today);
        if (Money.isPositive(fine.fine())) {
            chargeService.create(loan.getMember(), ChargeType.OVERDUE_FINE, fine.fine(), today, loan, null,
                    "Overdue fine: " + fine.chargeableDays() + " chargeable days");
        }
        // 3. route the copy (section 8.6)
        RoutingResult routed = routing.route(copy, branch, today, true);

        return new ReturnReceipt(copy.getBarcode(), copy.getTitle().getTitle(), today, loan.getMember().fullName(),
                fine.fine(), fine.chargeableDays(), routingInfo(routed), false, false, routingMessage(routed));
    }

    private ReturnReceipt returnLostCopy(Copy copy, Branch branch, LocalDate today) {
        var result = lostItems.returnLostCopy(copy, branch, today);
        String memberName = result.loan().getMember() == null ? null : result.loan().getMember().fullName();
        String message = result.replacementCancelled()
                ? "Lost copy returned: the replacement cost charge was cancelled (refunded if paid). Fine and fee remain due."
                : "Lost copy returned after the refund window: checked in, nothing was cancelled or refunded.";
        return new ReturnReceipt(copy.getBarcode(), copy.getTitle().getTitle(), today, memberName, Money.ZERO, 0,
                routingInfo(result.routing()), true, result.replacementCancelled(),
                message + " " + routingMessage(result.routing()));
    }

    // ----- arrival after transfer -----

    /** F-LEN-06: a librarian at the destination branch checks in a copy that arrives after a transfer. */
    public ArrivalReceipt arrive(String barcode, CurrentUser staff) {
        LocalDate today = businessDate.today();
        Branch branch = branchOf(staff);
        Copy copy = copies.findByBarcode(barcode.trim()).orElseThrow(() -> new NotFoundException("Copy", barcode));
        RoutingResult routed = routing.arrive(copy, branch, today);
        return new ArrivalReceipt(copy.getBarcode(), copy.getTitle().getTitle(), routingInfo(routed), routingMessage(routed));
    }

    // ----- helpers -----

    private Branch branchOf(CurrentUser staff) {
        if (staff.branchId() == null) {
            throw new BusinessRuleException("NO_BRANCH", "F-ADM-04", "This staff account is not linked to a branch.");
        }
        return branches.findById(staff.branchId()).orElseThrow(() -> new NotFoundException("Branch", staff.branchId()));
    }

    private static RoutingInfo routingInfo(RoutingResult result) {
        return new RoutingInfo(result.outcome(), result.copyStatus(), result.destinationBranchCode());
    }

    private static String routingMessage(RoutingResult result) {
        return switch (result.outcome()) {
            case HOLD_HERE -> "The copy is on hold for a reservation at this branch.";
            case TRANSIT_TO_PICKUP -> "Send the copy to " + result.destinationBranchCode() + " for a reservation.";
            case AVAILABLE_HERE -> "The copy is available on the shelf.";
            case TRANSIT_TO_HOME -> "Send the copy to its home branch " + result.destinationBranchCode() + ".";
        };
    }
}
