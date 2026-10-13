package be.npl.shelfwise.calendar.service;

import be.npl.shelfwise.calendar.entity.PublicHoliday;
import be.npl.shelfwise.calendar.helper.CalendarRules;
import be.npl.shelfwise.calendar.repository.PublicHolidayRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database backed access to the library calendar (the same for all branches). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final PublicHolidayRepository holidays;

    /** A closing-day predicate that is valid for dates in [from, to]. */
    public Predicate<LocalDate> closingDays(LocalDate from, LocalDate to) {
        Set<LocalDate> dates = holidays.findByHolidayDateBetween(from, to).stream()
                .map(PublicHoliday::getHolidayDate)
                .collect(Collectors.toSet());
        return CalendarRules.closingDays(dates);
    }

    public boolean isClosingDay(LocalDate date) {
        return closingDays(date, date).test(date);
    }

    public LocalDate nextOpeningDay(LocalDate date) {
        return CalendarRules.nextOpeningDay(date, closingDays(date, date.plusDays(60)));
    }

    public int countOpeningDays(LocalDate fromExclusive, LocalDate toInclusive) {
        return CalendarRules.countOpeningDays(fromExclusive, toInclusive, closingDays(fromExclusive, toInclusive));
    }
}
