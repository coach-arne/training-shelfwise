package be.npl.shelfwise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.calendar.helper.CalendarRules;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.lending.helper.DueDateCalculator;
import be.npl.shelfwise.reservation.helper.HoldPeriodCalculator;
import be.npl.shelfwise.support.Holidays2026;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Calendar rules (section 5.1), due dates (A.1) and hold periods (A.7). */
class CalendarAndDueDateTest {

    @ParameterizedTest(name = "{0}")
    @DisplayName("A.1 due dates and renewals (pure calculation)")
    @CsvSource({
            "AC-LEN-01, BOOK,        2026-03-02, 2026-03-23",
            "AC-LEN-02, BOOK,        2026-03-16, 2026-04-07",
            "AC-LEN-03, AUDIOVISUAL, 2026-05-07, 2026-05-15",
            "AC-LEN-04, BOOK,        2026-03-23, 2026-04-13",
            "AC-LEN-05, BOOK,        2026-04-13, 2026-05-04",
            "AC-LEN-07, BOOK,        2026-03-16, 2026-04-07",
    })
    void dueDates(String id, MaterialType material, LocalDate base, LocalDate expected) {
        assertThat(DueDateCalculator.dueDate(base, material, Holidays2026.CLOSING)).as(id).isEqualTo(expected);
    }

    @Test
    @DisplayName("BR-LOA-02: a due date on a Sunday moves to Monday; the MAGAZINE loan period is 14 days")
    void sundayAndMagazine() {
        assertThat(DueDateCalculator.dueDate(LocalDate.of(2026, 3, 1), MaterialType.BOOK, Holidays2026.CLOSING))
                .isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(DueDateCalculator.dueDate(LocalDate.of(2026, 3, 2), MaterialType.MAGAZINE, Holidays2026.CLOSING))
                .isEqualTo(LocalDate.of(2026, 3, 16));
        // 2026-03-15 is a Sunday: a magazine checked out on Sunday 2026-03-01 is due on Sunday 2026-03-15 -> Monday.
        assertThat(DueDateCalculator.dueDate(LocalDate.of(2026, 3, 1), MaterialType.MAGAZINE, Holidays2026.CLOSING))
                .isEqualTo(LocalDate.of(2026, 3, 16));
    }

    @Test
    @DisplayName("Closing days are Sundays and public holidays; chargeable days skip them")
    void closingDays() {
        assertThat(Holidays2026.CLOSING.test(LocalDate.of(2026, 3, 8))).isTrue();
        assertThat(Holidays2026.CLOSING.test(LocalDate.of(2026, 4, 6))).isTrue();
        assertThat(Holidays2026.CLOSING.test(LocalDate.of(2026, 3, 9))).isFalse();
        assertThat(CalendarRules.countOpeningDays(LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 8), Holidays2026.CLOSING))
                .isEqualTo(2);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("A.7 hold period and pickup reminder")
    @CsvSource({
            "AC-RES-07, 2026-03-02, 2026-03-09, 2026-03-07",
            "AC-RES-08, 2026-05-07, 2026-05-15, 2026-05-12",
    })
    void holdPeriod(String id, LocalDate notified, LocalDate lastHoldDay, LocalDate reminder) {
        assertThat(HoldPeriodCalculator.lastHoldDay(notified, 7, Holidays2026.CLOSING)).as(id).isEqualTo(lastHoldDay);
        assertThat(HoldPeriodCalculator.reminderDay(notified, 5)).as(id).isEqualTo(reminder);
    }
}
