package be.npl.shelfwise.processing;

import be.npl.shelfwise.businessdate.service.BusinessDateService;
import be.npl.shelfwise.lending.service.LostItemService;
import be.npl.shelfwise.member.service.AnonymisationService;
import be.npl.shelfwise.notification.service.NotificationDraft;
import be.npl.shelfwise.notification.service.NotificationService;
import be.npl.shelfwise.reservation.service.ReservationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The once-per-day run that handles all time-based events (section 5.2). It runs for every calendar day, closing
 * days included. Missing runs are executed in date order before anything else (catch-up).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyProcessingService {

    private final BusinessDateService businessDate;
    private final LostItemService lostItems;
    private final ScheduledNoticeService notices;
    private final ReservationService reservations;
    private final AnonymisationService anonymisation;
    private final NotificationService notifications;
    private final TransactionTemplate transactions;

    /** Runs every date after the last processed date, up to and including {@code through}, in date order. */
    public DailyProcessingSummary catchUp(LocalDate through) {
        DailyProcessingSummary total = DailyProcessingSummary.empty();
        LocalDate next = businessDate.lastProcessedDate().plusDays(1);
        while (!next.isAfter(through)) {
            LocalDate date = next;
            total = total.plus(transactions.execute(status -> runDay(date)));
            next = next.plusDays(1);
        }
        return total;
    }

    /** The run for a single date; the steps follow a fixed order. */
    private DailyProcessingSummary runDay(LocalDate date) {
        log.info("Daily processing for {}", date);
        List<NotificationDraft> drafts = new ArrayList<>();

        int lost = lostItems.declareLostLoans(date, drafts);
        int overdueNotices = notices.overdueNotices(date, drafts);
        int dueReminders = notices.dueReminders(date, drafts);
        int expired = reservations.expireHolds(date, drafts);
        int pickupReminders = reservations.pickupReminders(date, drafts);
        int membershipReminders = notices.membershipReminders(date, drafts);
        int anonymised = anonymisation.anonymise(date);
        int messages = notifications.sendScheduled(drafts, date);

        businessDate.setLastProcessedDate(date);
        return new DailyProcessingSummary(List.of(date), dueReminders, overdueNotices, lost, expired,
                pickupReminders, membershipReminders, anonymised, messages);
    }
}
