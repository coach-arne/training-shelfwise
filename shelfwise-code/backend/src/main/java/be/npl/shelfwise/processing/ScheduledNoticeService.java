package be.npl.shelfwise.processing;

import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.finance.service.FineService;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.notification.entity.NotificationType;
import be.npl.shelfwise.notification.service.NotificationDraft;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The time based notifications of F-NOT-02 that daily processing creates: N-01 to N-04 (only for loans that are
 * still open on that day) and N-09. All timings are in calendar days.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduledNoticeService {

    private final LoanRepository loans;
    private final MemberRepository members;
    private final FineService fines;
    private final RulesProperties rules;

    public int dueReminders(LocalDate today, List<NotificationDraft> drafts) {
        List<Loan> due = loans.findByStatusAndDueDate(LoanStatus.OPEN, today.plusDays(rules.dueReminderDaysBefore()));
        due.forEach(l -> drafts.add(new NotificationDraft(l.getMember(), NotificationType.N_01, List.of(
                "\"" + l.getCopy().getTitle().getTitle() + "\" is due on " + l.getDueDate() + "."))));
        return due.size();
    }

    public int overdueNotices(LocalDate today, List<NotificationDraft> drafts) {
        return overdue(today, rules.firstOverdueNoticeDays(), NotificationType.N_02, drafts)
                + overdue(today, rules.secondOverdueNoticeDays(), NotificationType.N_03, drafts)
                + overdue(today, rules.thirdOverdueNoticeDays(), NotificationType.N_04, drafts);
    }

    public int membershipReminders(LocalDate today, List<NotificationDraft> drafts) {
        // The expiry date is the "valid until" date of the membership (its last valid day).
        var expiring = members.findByMembershipEnd(today.plusDays(rules.membershipReminderDays()));
        expiring.forEach(m -> drafts.add(new NotificationDraft(m, NotificationType.N_09, List.of(
                "The membership of " + m.fullName() + " is valid until " + m.getMembershipEnd()
                        + ". You can renew it at the desk or in the member portal."))));
        return expiring.size();
    }

    private int overdue(LocalDate today, int daysAfterDue, NotificationType type, List<NotificationDraft> drafts) {
        List<Loan> overdue = loans.findByStatusAndDueDate(LoanStatus.OPEN, today.minusDays(daysAfterDue));
        for (Loan loan : overdue) {
            FineResult accrued = fines.fineFor(loan, today);
            drafts.add(new NotificationDraft(loan.getMember(), type, List.of(
                    "\"" + loan.getCopy().getTitle().getTitle() + "\" was due on " + loan.getDueDate()
                            + " and is overdue. Fine accrued so far: EUR " + accrued.fine() + " ("
                            + accrued.chargeableDays() + " chargeable days).")));
        }
        return overdue.size();
    }
}
