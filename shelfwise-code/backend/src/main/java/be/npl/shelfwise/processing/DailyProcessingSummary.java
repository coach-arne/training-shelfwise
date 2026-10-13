package be.npl.shelfwise.processing;

import java.time.LocalDate;
import java.util.List;

/** What the daily processing did (F-TRN-02). */
public record DailyProcessingSummary(
        List<LocalDate> datesProcessed,
        int dueReminders,
        int overdueNotices,
        int copiesDeclaredLost,
        int holdsExpired,
        int pickupReminders,
        int membershipReminders,
        int loansAnonymised,
        int messagesWritten) {

    public static DailyProcessingSummary empty() {
        return new DailyProcessingSummary(List.of(), 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public DailyProcessingSummary plus(DailyProcessingSummary other) {
        List<LocalDate> dates = new java.util.ArrayList<>(datesProcessed);
        dates.addAll(other.datesProcessed);
        return new DailyProcessingSummary(dates, dueReminders + other.dueReminders,
                overdueNotices + other.overdueNotices, copiesDeclaredLost + other.copiesDeclaredLost,
                holdsExpired + other.holdsExpired, pickupReminders + other.pickupReminders,
                membershipReminders + other.membershipReminders, loansAnonymised + other.loansAnonymised,
                messagesWritten + other.messagesWritten);
    }
}
