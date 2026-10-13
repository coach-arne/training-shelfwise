package be.npl.shelfwise.support;

import be.npl.shelfwise.calendar.helper.CalendarRules;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

/** The public holidays of 2026 (section 5.1) for the unit tests that run without a database. */
public final class Holidays2026 {

    public static final Set<LocalDate> DATES = Set.of(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 6), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 14),
            LocalDate.of(2026, 5, 25), LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 15), LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 11), LocalDate.of(2026, 12, 25));

    public static final Predicate<LocalDate> CLOSING = CalendarRules.closingDays(DATES);

    private Holidays2026() {
    }
}
