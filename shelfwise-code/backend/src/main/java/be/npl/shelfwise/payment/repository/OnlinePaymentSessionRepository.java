package be.npl.shelfwise.payment.repository;

import be.npl.shelfwise.payment.entity.OnlinePaymentSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnlinePaymentSessionRepository extends JpaRepository<OnlinePaymentSession, Long> {

    Optional<OnlinePaymentSession> findByReference(String reference);

    /** Row lock so that two simultaneous provider callbacks for the same payment are processed one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OnlinePaymentSession s where s.reference = :reference")
    Optional<OnlinePaymentSession> findByReferenceForUpdate(@Param("reference") String reference);
}
