package be.npl.shelfwise.admin.service;

import be.npl.shelfwise.admin.web.AdminDtos.HolidayRequest;
import be.npl.shelfwise.admin.web.AdminDtos.HolidayView;
import be.npl.shelfwise.calendar.entity.PublicHoliday;
import be.npl.shelfwise.calendar.repository.PublicHolidayRepository;
import be.npl.shelfwise.common.BusinessRuleException;
import be.npl.shelfwise.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-ADM-02: add, remove and view public holidays per year. They are closing days for all branches. */
@Service
@RequiredArgsConstructor
@Transactional
public class HolidayAdminService {

    private final PublicHolidayRepository holidays;

    @Transactional(readOnly = true)
    public List<HolidayView> listForYear(int year) {
        return holidays.findByHolidayDateBetween(java.time.LocalDate.of(year, 1, 1), java.time.LocalDate.of(year, 12, 31))
                .stream()
                .sorted(java.util.Comparator.comparing(PublicHoliday::getHolidayDate))
                .map(h -> new HolidayView(h.getId(), h.getHolidayDate(), h.getName()))
                .toList();
    }

    public HolidayView add(HolidayRequest request) {
        if (holidays.existsByHolidayDate(request.date())) {
            throw new BusinessRuleException("DUPLICATE_HOLIDAY", "F-ADM-02", "That date is already a public holiday.");
        }
        PublicHoliday holiday = new PublicHoliday();
        holiday.setHolidayDate(request.date());
        holiday.setName(request.name().trim());
        holidays.save(holiday);
        return new HolidayView(holiday.getId(), holiday.getHolidayDate(), holiday.getName());
    }

    public void remove(Long id) {
        PublicHoliday holiday = holidays.findById(id).orElseThrow(() -> new NotFoundException("Holiday", id));
        holidays.delete(holiday);
    }
}
