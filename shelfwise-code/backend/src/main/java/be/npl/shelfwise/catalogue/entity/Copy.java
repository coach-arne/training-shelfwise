package be.npl.shelfwise.catalogue.entity;

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
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Title title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Branch homeBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Branch currentBranch;

    /** Set while the copy is IN_TRANSIT. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Branch transitDestination;

    private String shelfMark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CopyStatus status;

    /** Optional; the default of the material type applies when empty (F-CAT-01). */
    private BigDecimal replacementCost;

    @Column(nullable = false)
    private LocalDate acquisitionDate;

    public BigDecimal effectiveReplacementCost() {
        return replacementCost != null ? replacementCost : title.getMaterialType().defaultReplacementCost();
    }
}
