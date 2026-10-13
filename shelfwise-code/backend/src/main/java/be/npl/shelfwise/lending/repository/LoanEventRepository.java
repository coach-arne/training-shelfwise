package be.npl.shelfwise.lending.repository;

import be.npl.shelfwise.lending.entity.LoanEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {

    @Query("select e from LoanEvent e left join fetch e.branch where e.eventDate = :date")
    List<LoanEvent> findByEventDateWithBranch(@Param("date") LocalDate date);
}
