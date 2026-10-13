package be.npl.shelfwise.payment.entity;

import be.npl.shelfwise.member.entity.Member;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A payment that is in progress at the (simulated) payment provider. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class OnlinePaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSessionStatus status;

    @Column(nullable = false)
    private LocalDate createdOn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "online_payment_session_charge", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "charge_id")
    private Set<Long> chargeIds = new HashSet<>();
}
