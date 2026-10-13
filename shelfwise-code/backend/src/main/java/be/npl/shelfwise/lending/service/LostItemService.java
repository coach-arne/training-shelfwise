package be.npl.shelfwise.lending.service;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.repository.CopyRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.service.ChargeService;
import be.npl.shelfwise.finance.service.FineService;
import be.npl.shelfwise.lending.entity.Channel;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanEventType;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.lending.service.CopyRoutingService.RoutingResult;
import be.npl.shelfwise.notification.entity.NotificationType;
import be.npl.shelfwise.notification.service.NotificationDraft;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lost declaration (F-FIN-08) and return of a lost copy (F-FIN-09). */
@Service
@RequiredArgsConstructor
@Transactional
public class LostItemService {

    private final LoanRepository loans;
    private final CopyRepository copies;
    private final ChargeRepository charges;
    private final ChargeService chargeService;
    private final FineService fines;
    private final CopyRoutingService routing;
    private final LoanEventService loanEvents;
    private final RulesProperties rules;

    public record LostReturn(Loan loan, boolean replacementCancelled, RoutingResult routing) {
    }

    /**
     * BR-FIN-07: every open loan whose due date is 42 calendar days ago (or more, after a catch-up) is declared lost.
     * N-05 is added to {@code drafts}.
     */
    public int declareLostLoans(LocalDate today, List<NotificationDraft> drafts) {
        List<Loan> due = loans.findByStatusAndDueDateLessThanEqual(LoanStatus.OPEN, today.minusDays(rules.lostAfterDays()));
        due.forEach(loan -> declareLost(loan, today, drafts));
        return due.size();
    }

    private void declareLost(Loan loan, LocalDate today, List<NotificationDraft> drafts) {
        Copy copy = loan.getCopy();
        copy.setStatus(CopyStatus.LOST);
        copies.save(copy);
        loan.setStatus(LoanStatus.LOST);
        loan.setLostDate(today);
        loans.save(loan);

        List<String> lines = new ArrayList<>();
        lines.add("\"" + copy.getTitle().getTitle() + "\" (copy " + copy.getBarcode() + ") has been declared lost.");
        FineResult fine = fines.fineFor(loan, today);
        BigDecimal total = BigDecimal.ZERO;
        if (fine.fine().signum() > 0) {
            chargeService.create(loan.getMember(), ChargeType.OVERDUE_FINE, fine.fine(), today, loan, null,
                    "Overdue fine: " + fine.chargeableDays() + " chargeable days");
            lines.add("Overdue fine: EUR " + fine.fine() + " (" + fine.chargeableDays() + " chargeable days)");
            total = total.add(fine.fine());
        }
        BigDecimal replacement = copy.effectiveReplacementCost();
        chargeService.create(loan.getMember(), ChargeType.LOST_REPLACEMENT, replacement, today, loan, null,
                "Replacement cost of \"" + copy.getTitle().getTitle() + "\"");
        lines.add("Replacement cost: EUR " + replacement);
        chargeService.create(loan.getMember(), ChargeType.LOST_ADMIN_FEE, rules.lostAdminFee(), today, loan, null,
                "Administration fee for a lost copy");
        lines.add("Administration fee: EUR " + rules.lostAdminFee());
        total = total.add(replacement).add(rules.lostAdminFee());
        lines.add("Total charged: EUR " + total);
        drafts.add(new NotificationDraft(loan.getMember(), NotificationType.N_05, lines));
    }

    /**
     * BR-FIN-10: a lost copy returned within 90 days of the declaration (the 90th day included) cancels the
     * replacement cost charge, with a refund of anything already paid. BR-FIN-13: later, nothing is cancelled.
     * In both cases the copy is routed according to section 8.6.
     */
    public LostReturn returnLostCopy(Copy copy, Branch branch, LocalDate today) {
        Loan loan = loans.findFirstByCopyIdAndStatusOrderByIdDesc(copy.getId(), LoanStatus.LOST)
                .orElseThrow(() -> new BusinessRuleException("COPY_NOT_ON_LOAN", "BR-FIN-10",
                        "Copy " + copy.getBarcode() + " is lost but has no lost loan."));
        boolean withinWindow = ChronoUnit.DAYS.between(loan.getLostDate(), today) <= rules.lostRefundWindowDays();
        boolean cancelled = false;
        if (withinWindow) {
            Charge replacement = charges.findFirstByLoanIdAndTypeOrderByIdDesc(loan.getId(), ChargeType.LOST_REPLACEMENT)
                    .orElse(null);
            if (replacement != null) {
                chargeService.cancel(replacement, today, "Lost copy returned within " + rules.lostRefundWindowDays() + " days");
                cancelled = true;
            }
        }
        loan.setReturnDate(today);
        loan.setReturnBranch(branch);
        loans.save(loan);
        loanEvents.record(loan, LoanEventType.RETURN, today, branch, Channel.DESK);
        return new LostReturn(loan, cancelled, routing.route(copy, branch, today, true));
    }
}
