package be.npl.shelfwise.calendar.repository;

import be.npl.shelfwise.calendar.entity.PublicHoliday;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    List<PublicHoliday> findByHolidayDateBetween(LocalDate from, LocalDate to);

    List<PublicHoliday> findAllByOrderByHolidayDate();

    boolean existsByHolidayDate(LocalDate date);
}
