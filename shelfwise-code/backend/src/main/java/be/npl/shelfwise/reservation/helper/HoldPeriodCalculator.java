package be.npl.shelfwise.reservation.helper;

import be.npl.shelfwise.calendar.helper.CalendarRules;
import java.time.LocalDate;
import java.util.function.Predicate;

public final class HoldPeriodCalculator {

    private HoldPeriodCalculator() {
    }

    /**
     * BR-RES-02, BR-RES-06: the copy is held for 7 days after the pickup notification; when the last hold day is a
     * closing day, the hold is extended to the next opening day.
     */
    public static LocalDate lastHoldDay(LocalDate notificationDate, int holdDays, Predicate<LocalDate> closingDay) {
        return CalendarRules.nextOpeningDay(notificationDate.plusDays(holdDays), closingDay);
    }

    /** The pickup reminder N-07 is sent 5 days after the pickup notification. */
    public static LocalDate reminderDay(LocalDate notificationDate, int reminderDays) {
        return notificationDate.plusDays(reminderDays);
    }
}
