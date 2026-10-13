package be.npl.shelfwise.lending.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.finance.service.FineService;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of loans: views with due dates, renewals left and the accrued fine (F-MEM-07, F-FIN-07). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanQueryService {

    private final LoanRepository loans;
    private final FineService fines;
    private final RenewalPolicy renewalPolicy;
    private final BusinessDateService businessDate;
    private final RulesProperties rules;

    public List<LoanView> openLoansOf(Long memberId) {
        LocalDate today = businessDate.today();
        return loans.findByMemberIdAndStatusOrderByDueDateAscIdAsc(memberId, LoanStatus.OPEN).stream()
                .map(l -> toView(l, today)).toList();
    }

    /** Loan history as far as it has not been anonymised (F-MEM-07). */
    public List<LoanView> historyOf(Long memberId) {
        LocalDate today = businessDate.today();
        return loans.findByMemberIdOrderByCheckoutDateDescIdDesc(memberId).stream()
                .filter(l -> !l.isOpen())
                .map(l -> toView(l, today)).toList();
    }

    public LoanView toView(Loan loan, LocalDate today) {
        boolean overdue = loan.isOverdueOn(today);
        FineResult accrued = overdue ? fines.fineFor(loan, today) : new FineResult(0, Money.ZERO);
        Optional<BusinessRuleException> refusal = loan.isOpen() ? renewalPolicy.refusal(loan, today) : Optional.empty();
        return new LoanView(loan.getId(), loan.getCopy().getId(), loan.getCopy().getBarcode(),
                loan.getCopy().getTitle().getId(), loan.getCopy().getTitle().getTitle(),
                loan.getCopy().getTitle().getMaterialType(),
                loan.getMember() == null ? null : loan.getMember().getId(),
                loan.getMember() == null ? null : loan.getMember().fullName(), loan.getCheckoutDate(),
                loan.getDueDate(), loan.getReturnDate(), loan.getStatus(), loan.getRenewalCount(),
                Math.max(0, rules.maxRenewals() - loan.getRenewalCount()), overdue, accrued.chargeableDays(),
                accrued.fine(), loan.getCheckoutBranch().getCode(), loan.isOpen() && refusal.isEmpty(),
                refusal.map(BusinessRuleException::getMessage).orElse(null));
    }
}
