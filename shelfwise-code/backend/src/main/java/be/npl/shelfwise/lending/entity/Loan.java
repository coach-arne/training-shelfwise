package be.npl.shelfwise.lending.entity;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.entity.MemberCategory;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Copy copy;

    /** Empty once the loan has been anonymised (BR-MEM-05). */
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    /** Category at checkout, kept for statistics after anonymisation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCategory memberCategory;

    @Column(nullable = false)
    private LocalDate checkoutDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private int renewalCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Branch checkoutBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    private Branch returnBranch;

    private LocalDate lostDate;

    @Column(nullable = false)
    private boolean anonymised;

    public boolean isOpen() {
        return status == LoanStatus.OPEN;
    }

    public boolean isOverdueOn(LocalDate businessDate) {
        return isOpen() && businessDate.isAfter(dueDate);
    }
}
