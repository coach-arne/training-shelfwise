package be.npl.shelfwise.member.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maximum simultaneous loans per category (BR-MEM-02), stored so that OD-01 can change without rework. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class LoanLimit {

    @Id
    @Enumerated(EnumType.STRING)
    private MemberCategory category;

    private int maxLoans;

    public LoanLimit(MemberCategory category, int maxLoans) {
        this.category = category;
        this.maxLoans = maxLoans;
    }
}
