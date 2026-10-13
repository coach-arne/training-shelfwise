package be.npl.shelfwise.member.entity;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.security.StaffAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A membership fee paid at registration or renewal. Not a charge and not part of the balance (BR-MEM-04). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class MembershipFeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Column(nullable = false)
    private LocalDate paidOn;

    @Column(nullable = false)
    private String kind;

    @ManyToOne(fetch = FetchType.LAZY)
    private StaffAccount staff;

    @ManyToOne(fetch = FetchType.LAZY)
    private Branch branch;
}
