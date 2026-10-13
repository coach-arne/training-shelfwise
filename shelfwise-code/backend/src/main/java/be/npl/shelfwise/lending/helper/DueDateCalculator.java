package be.npl.shelfwise.lending.helper;

import be.npl.shelfwise.calendar.helper.CalendarRules;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import java.time.LocalDate;
import java.util.function.Predicate;

public final class DueDateCalculator {

    private DueDateCalculator() {
    }

    /**
     * Due date = base date + loan period (BR-LOA-01, F-LEN-03); when it falls on a closing day it moves to the next
     * opening day (BR-LOA-02). At checkout the base date is the checkout date; for a renewal it is the current due
     * date, not the renewal date (BR-LOA-04).
     */
    public static LocalDate dueDate(LocalDate baseDate, MaterialType material, Predicate<LocalDate> closingDay) {
        return CalendarRules.nextOpeningDay(baseDate.plusDays(material.loanPeriodDays()), closingDay);
    }
}
