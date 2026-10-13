package be.npl.shelfwise.payment.repository;

import be.npl.shelfwise.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderReference(String providerReference);

    List<Payment> findByMemberIdOrderByIdDesc(Long memberId);
}
