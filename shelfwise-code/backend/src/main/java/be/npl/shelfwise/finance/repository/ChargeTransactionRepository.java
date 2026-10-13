package be.npl.shelfwise.finance.repository;

import be.npl.shelfwise.finance.entity.ChargeTransaction;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeTransactionRepository extends JpaRepository<ChargeTransaction, Long> {

    List<ChargeTransaction> findByChargeIdOrderByIdAsc(Long chargeId);

    List<ChargeTransaction> findByChargeIdIn(Collection<Long> chargeIds);
}
