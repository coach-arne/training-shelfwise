package be.npl.shelfwise.finance.entity;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.security.Role;
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
public class Waiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Charge charge;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private StaffAccount staff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role staffRole;

    @ManyToOne(fetch = FetchType.LAZY)
    private Branch branch;

    @Column(nullable = false)
    private LocalDate waiverDate;
}
