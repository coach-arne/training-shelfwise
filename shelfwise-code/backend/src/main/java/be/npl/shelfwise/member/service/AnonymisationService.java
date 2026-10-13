package be.npl.shelfwise.member.service;

import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.repository.LoanRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-MEM-05: a returned loan is anonymised 12 months after its return date: the link between the loan and the member
 * is removed. Loans of members who keep their reading history, and loans with an outstanding charge, are skipped.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnonymisationService {

    private final LoanRepository loans;
    private final RulesProperties rules;

    /** @return the number of loans that were anonymised on {@code date} */
    public int anonymise(LocalDate date) {
        List<Loan> due = loans.findAnonymisable(date.minusMonths(rules.anonymiseAfterMonths()));
        for (Loan loan : due) {
            loan.setMember(null);
            loan.setAnonymised(true);
            loans.save(loan);
        }
        return due.size();
    }
}
