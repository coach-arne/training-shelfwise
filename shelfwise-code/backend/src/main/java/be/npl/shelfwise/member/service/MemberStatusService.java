package be.npl.shelfwise.member.service;

import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.member.entity.LoanLimit;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.helper.MembershipPeriod;
import be.npl.shelfwise.member.repository.LoanLimitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account balance, blocking and borrowing entitlements (F-MEM-05, F-MEM-06). Blocking is always computed, never
 * stored, so the block is lifted immediately when the balance drops below the threshold (BR-MEM-03).
 */
@Service
@RequiredArgsConstructor
// A refused check must not mark the surrounding transaction rollback-only: RenewalPolicy turns the refusal
// into a result (renewal not possible) without failing the request.
@Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
public class MemberStatusService {

    private final ChargeRepository charges;
    private final LoanRepository loans;
    private final LoanLimitRepository loanLimits;
    private final RulesProperties rules;

    public record BlockReason(String code, String message) {
    }

    public record BlockStatus(boolean blocked, boolean membershipValid, BigDecimal balance, List<BlockReason> reasons) {
    }

    public BigDecimal balance(Long memberId) {
        return Money.round(charges.sumOutstanding(memberId, ChargeStatus.OPEN));
    }

    public boolean isMembershipValid(Member member, LocalDate today) {
        return MembershipPeriod.isValidOn(member.getMembershipEnd(), today);
    }

    public BlockStatus blockStatus(Member member, LocalDate today) {
        BigDecimal balance = balance(member.getId());
        boolean valid = isMembershipValid(member, today);
        List<BlockReason> reasons = new ArrayList<>();
        if (!valid) {
            reasons.add(new BlockReason("MEMBERSHIP_EXPIRED", "The membership expired on " + member.getMembershipEnd() + "."));
        }
        if (Money.gte(balance, rules.blockingThreshold())) {
            reasons.add(new BlockReason("BALANCE_TOO_HIGH",
                    "The account balance is EUR " + balance + " (blocked from EUR " + rules.blockingThreshold() + ")."));
        }
        return new BlockStatus(!reasons.isEmpty(), valid, balance, reasons);
    }

    /** First check of a checkout, also used for renewals and reservations (BR-MEM-03). */
    public void assertMembershipValid(Member member, LocalDate today) {
        if (!isMembershipValid(member, today)) {
            throw new BusinessRuleException("MEMBERSHIP_EXPIRED", "BR-MEM-03",
                    "The membership expired on " + member.getMembershipEnd() + ".");
        }
    }

    /** Second check of a checkout: the balance must be below the blocking threshold (BR-MEM-03). */
    public void assertNotBlockedByBalance(Member member) {
        BigDecimal balance = balance(member.getId());
        if (Money.gte(balance, rules.blockingThreshold())) {
            throw new BusinessRuleException("MEMBER_BLOCKED", "BR-MEM-03",
                    "The member is blocked: the account balance is EUR " + balance + ".");
        }
    }

    /** Membership valid and balance below the threshold: required to borrow, renew or reserve. */
    public void assertMayUseLending(Member member, LocalDate today) {
        assertMembershipValid(member, today);
        assertNotBlockedByBalance(member);
    }

    public int maxLoans(MemberCategory category) {
        return loanLimits.findById(category).map(LoanLimit::getMaxLoans).orElseThrow(
                () -> new IllegalStateException("No loan limit configured for " + category));
    }

    public long openLoanCount(Long memberId) {
        return loans.countByMemberIdAndStatus(memberId, LoanStatus.OPEN);
    }

    /** Third check of a checkout (BR-MEM-02). Lost loans are closed and do not count (BR-FIN-12). */
    public void assertBelowLoanLimit(Member member) {
        int max = maxLoans(member.getCategory());
        if (openLoanCount(member.getId()) >= max) {
            throw new BusinessRuleException("LOAN_LIMIT_REACHED", "BR-MEM-02",
                    "The maximum number of simultaneous loans (" + max + ") has been reached.");
        }
    }
}
