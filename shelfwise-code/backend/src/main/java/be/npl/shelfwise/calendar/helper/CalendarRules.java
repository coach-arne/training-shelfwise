package be.npl.shelfwise.calendar.helper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

/** Pure calendar rules of section 5.1: closing days are Sundays and public holidays. */
public final class CalendarRules {

    private CalendarRules() {
    }

    public static Predicate<LocalDate> closingDays(Set<LocalDate> publicHolidays) {
        return date -> date.getDayOfWeek() == DayOfWeek.SUNDAY || publicHolidays.contains(date);
    }

    /** The date itself when it is an opening day, otherwise the next opening day (BR-LOA-02, BR-RES-06). */
    public static LocalDate nextOpeningDay(LocalDate date, Predicate<LocalDate> closingDay) {
        LocalDate candidate = date;
        while (closingDay.test(candidate)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    /** Number of opening days in the range (fromExclusive, toInclusive]. */
    public static int countOpeningDays(LocalDate fromExclusive, LocalDate toInclusive, Predicate<LocalDate> closingDay) {
        int count = 0;
        for (LocalDate d = fromExclusive.plusDays(1); !d.isAfter(toInclusive); d = d.plusDays(1)) {
            if (!closingDay.test(d)) {
                count++;
            }
        }
        return count;
    }
}
