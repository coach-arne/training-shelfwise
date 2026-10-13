package be.npl.shelfwise.finance.repository;

import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.ChargeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargeRepository extends JpaRepository<Charge, Long> {

    List<Charge> findByMemberIdOrderByChargeDateDescIdDesc(Long memberId);

    /** Open charges of a member, oldest first (BR-FIN-09). */
    List<Charge> findByMemberIdAndStatusOrderByChargeDateAscIdAsc(Long memberId, ChargeStatus status);

    @Query("select coalesce(sum(c.outstandingAmount), 0) from Charge c where c.member.id = :memberId and c.status = :status")
    BigDecimal sumOutstanding(@Param("memberId") Long memberId, @Param("status") ChargeStatus status);

    Optional<Charge> findFirstByLoanIdAndTypeOrderByIdDesc(Long loanId, ChargeType type);

    Optional<Charge> findFirstByReservationIdAndTypeOrderByIdDesc(Long reservationId, ChargeType type);

    List<Charge> findByLoanId(Long loanId);

    List<Charge> findByType(ChargeType type);
}
