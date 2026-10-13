package be.npl.shelfwise.lending.service;

import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-LOA-05: when a renewal is refused. The same rules apply in the portal and at the desk, so both channels use
 * this single implementation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RenewalPolicy {

    private final ReservationRepository reservations;
    private final MemberStatusService memberStatus;
    private final RulesProperties rules;

    /** The first reason why the loan cannot be renewed, or empty when it can. */
    public Optional<BusinessRuleException> refusal(Loan loan, LocalDate today) {
        if (!loan.isOpen() || loan.getMember() == null) {
            return Optional.of(new BusinessRuleException("LOAN_NOT_OPEN", "BR-LOA-05", "The loan is not open."));
        }
        // Reservations that are on hold, or whose copy is already in transit to them, do not count.
        if (!reservations.findByTitleIdAndStatusAndAssignedCopyIsNullOrderByPlacedAtAscIdAsc(
                loan.getCopy().getTitle().getId(), ReservationStatus.WAITING).isEmpty()) {
            return Optional.of(new BusinessRuleException("RENEWAL_RESERVATIONS_WAITING", "BR-LOA-05",
                    "The loan cannot be renewed: other members are waiting for this title."));
        }
        try {
            memberStatus.assertMayUseLending(loan.getMember(), today);
        } catch (BusinessRuleException e) {
            return Optional.of(new BusinessRuleException(e.getCode(), "BR-LOA-05",
                    "The loan cannot be renewed: " + lowerFirst(e.getMessage())));
        }
        if (loan.isOverdueOn(today)) {
            return Optional.of(new BusinessRuleException("LOAN_OVERDUE", "BR-LOA-05",
                    "The loan cannot be renewed: it is overdue."));
        }
        if (loan.getRenewalCount() >= rules.maxRenewals()) {
            return Optional.of(new BusinessRuleException("MAX_RENEWALS_REACHED", "BR-LOA-03",
                    "The loan cannot be renewed: the maximum number of renewals (" + rules.maxRenewals()
                            + ") has been reached."));
        }
        if (loan.getCopy().getStatus() != CopyStatus.ON_LOAN) {
            return Optional.of(new BusinessRuleException("LOAN_NOT_OPEN", "BR-LOA-05", "The copy is not on loan."));
        }
        return Optional.empty();
    }

    private static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
