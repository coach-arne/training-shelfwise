package be.npl.shelfwise.finance.service;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.Money;
import be.npl.shelfwise.common.NotFoundException;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.Waiver;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.repository.WaiverRepository;
import be.npl.shelfwise.finance.web.FinanceDtos.ChargeView;
import be.npl.shelfwise.security.CurrentUser;
import be.npl.shelfwise.security.Role;
import be.npl.shelfwise.security.StaffAccount;
import be.npl.shelfwise.security.StaffAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Waivers (F-FIN-10): reason required, librarian limit per charge, own-account rule, no waivers by administrators. */
@Service
@RequiredArgsConstructor
@Transactional
public class WaiverService {

    private final ChargeRepository charges;
    private final WaiverRepository waivers;
    private final StaffAccountRepository staffAccounts;
    private final ChargeService chargeService;
    private final BusinessDateService businessDate;
    private final RulesProperties rules;

    public ChargeView waive(Long chargeId, BigDecimal requestedAmount, String reason, CurrentUser user) {
        if (user.role() == Role.ADMINISTRATOR || user.role() == Role.MEMBER) {
            throw new AccessDeniedException("Only librarians and branch managers can waive charges");
        }
        Charge charge = charges.findById(chargeId).orElseThrow(() -> new NotFoundException("Charge", chargeId));
        StaffAccount staff = staffAccounts.findById(user.accountId())
                .orElseThrow(() -> new NotFoundException("Staff account", user.accountId()));
        BigDecimal amount = Money.round(requestedAmount);

        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("REASON_REQUIRED", "F-FIN-10", "A reason is required for a waiver.");
        }
        if (staff.getLinkedMember() != null && staff.getLinkedMember().getId().equals(charge.getMember().getId())) {
            throw new BusinessRuleException("OWN_ACCOUNT_WAIVER", "F-FIN-10",
                    "A staff member cannot waive a charge on their own member account.");
        }
        if (charge.getStatus() != ChargeStatus.OPEN) {
            throw new BusinessRuleException("CHARGE_NOT_OPEN", "F-FIN-10", "Only an open charge can be waived.");
        }
        if (!Money.isPositive(amount) || Money.gt(amount, charge.getOutstandingAmount())) {
            throw new BusinessRuleException("INVALID_WAIVER_AMOUNT", "F-FIN-10",
                    "The waived amount must be between 0.01 and the outstanding amount of EUR "
                            + charge.getOutstandingAmount() + ".");
        }
        if (user.role() == Role.LIBRARIAN) {
            BigDecimal alreadyWaived = waivers.sumByChargeAndRole(chargeId, Role.LIBRARIAN);
            if (Money.gt(alreadyWaived.add(amount), rules.librarianWaiverLimit())) {
                throw new BusinessRuleException("LIBRARIAN_WAIVER_LIMIT", "BR-FIN-08",
                        "A librarian can waive at most EUR " + rules.librarianWaiverLimit() + " per charge in total"
                                + " (EUR " + alreadyWaived + " was already waived on this charge).");
            }
        }
        LocalDate today = businessDate.today();
        Waiver waiver = new Waiver();
        waiver.setCharge(charge);
        waiver.setAmount(amount);
        waiver.setReason(reason.trim());
        waiver.setStaff(staff);
        waiver.setStaffRole(user.role());
        waiver.setBranch(staff.getBranch());
        waiver.setWaiverDate(today);
        waivers.save(waiver);
        chargeService.applyWaiver(charge, amount, today, "Waived by " + staff.getUsername() + ": " + reason.trim());
        return chargeService.toView(charge);
    }
}
