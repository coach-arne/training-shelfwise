package be.npl.shelfwise.member.helper;

import java.time.LocalDate;

/** Pure rules for membership periods (F-MEM-04, BR-MEM-07). The end date is the last valid day (inclusive). */
public final class MembershipPeriod {

    public record Period(LocalDate start, LocalDate end) {
    }

    private MembershipPeriod() {
    }

    /** A membership that starts on {@code start} is valid for {@code months} months. */
    public static Period startingOn(LocalDate start, int months) {
        return new Period(start, start.plusMonths(months).minusDays(1));
    }

    /**
     * BR-MEM-07: on renewal the new period starts the day after the current period ends. If the membership has
     * already expired, the new period starts on the renewal date.
     */
    public static Period renewal(LocalDate currentEnd, LocalDate renewalDate, int months) {
        LocalDate start = renewalDate.isAfter(currentEnd) ? renewalDate : currentEnd.plusDays(1);
        return startingOn(start, months);
    }

    public static boolean isValidOn(LocalDate end, LocalDate date) {
        return !date.isAfter(end);
    }
}
