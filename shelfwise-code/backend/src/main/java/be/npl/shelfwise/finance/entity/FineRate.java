package be.npl.shelfwise.finance.entity;

import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.member.entity.MemberCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Daily overdue rate for a category and material type with a valid-from date (BR-FIN-02, BR-FIN-11).
 * Only ADULT and YOUTH rows exist; SENIOR uses the ADULT rate with a discount.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class FineRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialType materialType;

    @Column(nullable = false)
    private BigDecimal dailyRate;

    @Column(nullable = false)
    private LocalDate validFrom;
}
