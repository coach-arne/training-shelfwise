package be.npl.shelfwise.payment.repository;

import be.npl.shelfwise.payment.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
}
