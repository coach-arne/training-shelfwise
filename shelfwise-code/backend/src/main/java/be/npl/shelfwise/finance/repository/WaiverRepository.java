package be.npl.shelfwise.finance.repository;

import be.npl.shelfwise.finance.entity.Waiver;
import be.npl.shelfwise.security.Role;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaiverRepository extends JpaRepository<Waiver, Long> {

    /** Total waived on a charge by staff of the given role (BR-FIN-14). */
    @Query("select coalesce(sum(w.amount), 0) from Waiver w where w.charge.id = :chargeId and w.staffRole = :role")
    BigDecimal sumByChargeAndRole(@Param("chargeId") Long chargeId, @Param("role") Role role);
}
