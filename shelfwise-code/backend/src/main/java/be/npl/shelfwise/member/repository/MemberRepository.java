package be.npl.shelfwise.member.repository;

import be.npl.shelfwise.member.entity.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByCardNumber(String cardNumber);

    List<Member> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
            String firstName, String lastName, LocalDate birthDate);

    @Query("""
            select m from Member m
            where lower(m.firstName) like :pattern
               or lower(m.lastName) like :pattern
               or lower(concat(m.firstName, ' ', m.lastName)) like :pattern
               or lower(m.cardNumber) like :pattern
            order by m.lastName, m.firstName
            """)
    List<Member> search(@Param("pattern") String pattern, Pageable pageable);

    @Query(value = "select nextval('card_number_seq')", nativeQuery = true)
    long nextCardSequence();

    /** Members whose membership ends on the given date (for the expiry reminder N-09). */
    List<Member> findByMembershipEnd(LocalDate membershipEnd);
}
