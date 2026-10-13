package be.npl.shelfwise.member.repository;

import be.npl.shelfwise.member.entity.LoanLimit;
import be.npl.shelfwise.member.entity.MemberCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanLimitRepository extends JpaRepository<LoanLimit, MemberCategory> {
}
