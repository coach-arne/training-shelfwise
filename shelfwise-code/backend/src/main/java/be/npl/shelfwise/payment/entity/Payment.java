package be.npl.shelfwise.payment.entity;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.member.entity.Member;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private StaffAccount staff;

    @ManyToOne(fetch = FetchType.LAZY)
    private Branch branch;

    /** Unique: the same provider payment can never be recorded twice (AC-PAY-05). */
    @Column(unique = true)
    private String providerReference;
}
