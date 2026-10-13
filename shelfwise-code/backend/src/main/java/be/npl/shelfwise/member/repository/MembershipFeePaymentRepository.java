package be.npl.shelfwise.member.repository;

import be.npl.shelfwise.member.entity.MembershipFeePayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipFeePaymentRepository extends JpaRepository<MembershipFeePayment, Long> {
}
