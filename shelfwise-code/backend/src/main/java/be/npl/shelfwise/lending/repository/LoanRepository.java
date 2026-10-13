package be.npl.shelfwise.lending.repository;

import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findFirstByCopyIdAndStatusOrderByIdDesc(Long copyId, LoanStatus status);

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    List<Loan> findByMemberIdAndStatusOrderByDueDateAscIdAsc(Long memberId, LoanStatus status);

    List<Loan> findByMemberIdOrderByCheckoutDateDescIdDesc(Long memberId);

    List<Loan> findByStatusAndDueDate(LoanStatus status, LocalDate dueDate);

    List<Loan> findByStatusAndDueDateLessThanEqual(LoanStatus status, LocalDate dueDate);

    List<Loan> findByStatusAndDueDateLessThanOrderByDueDate(LoanStatus status, LocalDate before);

    @Query("""
            select count(l) > 0 from Loan l
            where l.member.id = :memberId
              and l.status = be.npl.shelfwise.lending.entity.LoanStatus.OPEN
              and l.copy.title.id = :titleId
            """)
    boolean memberHasOpenLoanOfTitle(@Param("memberId") Long memberId, @Param("titleId") Long titleId);

    /**
     * Returned loans that are due for anonymisation (BR-MEM-05): returned on or before the cutoff, the member has not
     * opted in to keep the reading history, and no charge of the loan has an outstanding amount.
     */
    @Query("""
            select l from Loan l
            where l.member is not null
              and l.returnDate is not null
              and l.returnDate <= :cutoff
              and l.member.keepHistory = false
              and not exists (
                  select c.id from Charge c
                  where c.loan = l
                    and c.status = be.npl.shelfwise.finance.entity.ChargeStatus.OPEN
                    and c.outstandingAmount > 0)
            order by l.id
            """)
    List<Loan> findAnonymisable(@Param("cutoff") LocalDate cutoff);

    List<Loan> findByStatusAndLostDateIsNotNullOrderByLostDateDesc(LoanStatus status);
}
