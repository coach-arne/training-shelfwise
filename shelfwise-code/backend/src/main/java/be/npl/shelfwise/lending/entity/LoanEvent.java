package be.npl.shelfwise.lending.entity;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One checkout, renewal or return, kept for report R-01. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class LoanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanEventType type;

    @Column(nullable = false)
    private LocalDate eventDate;

    /** Empty for online renewals. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialType materialType;
}
