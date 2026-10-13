package be.npl.shelfwise.finance.entity;

import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.reservation.entity.Reservation;
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
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeType type;

    @Column(nullable = false)
    private BigDecimal originalAmount;

    @Column(nullable = false)
    private BigDecimal outstandingAmount;

    @Column(nullable = false)
    private LocalDate chargeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    private Reservation reservation;

    private String description;
}
