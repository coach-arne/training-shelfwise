package be.npl.shelfwise;

import static be.npl.shelfwise.support.Rules.assertRefused;
import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.lending.service.LoanQueryService;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.service.MemberService;
import be.npl.shelfwise.member.web.MemberDtos.PreferencesRequest;
import be.npl.shelfwise.member.web.MemberDtos.RegisterMemberRequest;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.payment.service.PaymentService;
import be.npl.shelfwise.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Appendix A.2: members, limits, blocking and anonymisation. */
class MemberIT extends IntegrationTestBase {

    @Autowired LendingService lending;
    @Autowired LoanQueryService loanQueries;
    @Autowired ReservationService reservations;
    @Autowired PaymentService payments;
    @Autowired MemberService memberService;
    @Autowired LoanRepository loans;

    private void checkout(Member member, Copy copy) {
        lending.checkout(member.getCardNumber(), copy.getBarcode(), fx.librarianCen());
    }

    private Copy freshBook() {
        return fx.newCopy(MaterialType.BOOK, "CEN");
    }

    @Test
    @DisplayName("AC-MEM-01 ADULT with balance 4.99 can check out (seed S-06)")
    void notBlockedBelowThreshold() {
        Member finn = fx.member(6);
        assertThat(fx.balance(finn)).isEqualByComparingTo("4.99");
        Copy copy = freshBook();
        checkout(finn, copy);
        assertThat(loanQueries.openLoansOf(finn.getId())).hasSize(1);
    }

    @Test
    @DisplayName("AC-MEM-02 ADULT with balance 5.00 cannot check out, renew or reserve")
    void blockedAtThreshold() {
        Member ada = fx.member(1);
        LoanView loan = lending.checkout(ada.getCardNumber(), freshBook().getBarcode(), fx.librarianCen()).loan();
        Title reservable = fx.newTitle(MaterialType.BOOK);
        checkout(fx.member(4), fx.newCopyOf(reservable, "CEN"));
        fx.addCharge(ada, ChargeType.OVERDUE_FINE, "5.00", LocalDate.of(2026, 3, 16));

        assertRefused("MEMBER_BLOCKED", () -> lending.checkout(ada.getCardNumber(), freshBook().getBarcode(), fx.librarianCen()));
        assertRefused("MEMBER_BLOCKED", () -> lending.renew(loan.id(), fx.librarianCen()));
        assertRefused("MEMBER_BLOCKED", () -> reservations.place(ada.getId(), reservable.getId(), fx.branch("CEN").getId()));
    }

    @Test
    @DisplayName("AC-MEM-03 paying 0.01 lifts the block immediately (seed S-05)")
    void blockLiftedByPayment() {
        Member eva = fx.member(5);
        assertRefused("MEMBER_BLOCKED", () -> lending.checkout(eva.getCardNumber(), freshBook().getBarcode(), fx.librarianCen()));

        payments.deskPayment(eva.getId(), new BigDecimal("0.01"), PaymentMethod.CASH, fx.librarianCen());

        assertThat(fx.balance(eva)).isEqualByComparingTo("4.99");
        checkout(eva, freshBook());
        assertThat(loanQueries.openLoansOf(eva.getId())).hasSize(1);
    }

    @Test
    @DisplayName("AC-MEM-04 expired membership: return and payment allowed; checkout, renewal and reservation refused (seed S-07)")
    void expiredMembership() {
        Member gina = fx.member(7);
        fx.today(LocalDate.of(2026, 3, 2));
        Copy toReturn = freshBook();
        checkout(gina, toReturn);
        Copy toRenew = freshBook();
        checkout(gina, toRenew);
        Title reservable = fx.newTitle(MaterialType.BOOK);
        checkout(fx.member(1), fx.newCopyOf(reservable, "CEN"));
        fx.addCharge(gina, ChargeType.OVERDUE_FINE, "1.00", LocalDate.of(2026, 3, 2));

        fx.today(LocalDate.of(2026, 3, 11)); // the membership ended yesterday (2026-03-10)
        LoanView renewable = loanQueries.openLoansOf(gina.getId()).stream()
                .filter(l -> l.barcode().equals(toRenew.getBarcode())).findFirst().orElseThrow();

        lending.returnCopy(toReturn.getBarcode(), fx.librarianCen());
        payments.deskPayment(gina.getId(), new BigDecimal("1.00"), PaymentMethod.CASH, fx.librarianCen());

        assertRefused("MEMBERSHIP_EXPIRED", () -> lending.checkout(gina.getCardNumber(), freshBook().getBarcode(), fx.librarianCen()));
        assertRefused("MEMBERSHIP_EXPIRED", () -> lending.renew(renewable.id(), fx.librarianCen()));
        assertRefused("MEMBERSHIP_EXPIRED", () -> reservations.place(gina.getId(), reservable.getId(), fx.branch("CEN").getId()));
    }

    @Test
    @DisplayName("AC-MEM-05 ADULT with 10 open loans cannot borrow an 11th (seed S-02)")
    void adultLoanLimit() {
        Member ben = fx.member(2);
        assertThat(loanQueries.openLoansOf(ben.getId())).hasSize(10);
        assertRefused("LOAN_LIMIT_REACHED", () -> lending.checkout(ben.getCardNumber(), freshBook().getBarcode(), fx.librarianCen()));
    }

    @Test
    @DisplayName("AC-MEM-06 YOUTH with 5 open loans cannot borrow a 6th (seed S-03)")
    void youthLoanLimit() {
        Member cleo = fx.member(3);
        assertThat(loanQueries.openLoansOf(cleo.getId())).hasSize(5);
        assertRefused("LOAN_LIMIT_REACHED", () -> lending.checkout(cleo.getCardNumber(), freshBook().getBarcode(), fx.librarianCen()));
    }

    @Test
    @DisplayName("AC-MEM-07 category is set at registration and re-evaluated at membership renewal")
    void categoryAtRegistrationAndRenewal() {
        fx.today(LocalDate.of(2026, 3, 2));
        var request = new RegisterMemberRequest("Tim", "Test", LocalDate.of(2008, 6, 15), "Main Street 1", "1000",
                "Northwick", "tim.test@example.org", null, fx.member(4).getId(), null, null, PaymentMethod.CASH, false);
        var registered = memberService.register(request, fx.librarianCen());
        Long id = registered.member().id();
        assertThat(registered.member().category()).isEqualTo(MemberCategory.YOUTH);
        assertThat(registered.feePaid()).isEqualByComparingTo("0.00");

        fx.today(LocalDate.of(2026, 6, 15));
        assertThat(memberService.get(id).category()).isEqualTo(MemberCategory.YOUTH);

        fx.today(LocalDate.of(2027, 3, 2));
        var renewal = memberService.renewAtDesk(id, PaymentMethod.CASH, fx.librarianCen());
        assertThat(renewal.member().category()).isEqualTo(MemberCategory.ADULT);
        assertThat(renewal.feePaid()).isEqualByComparingTo("12.00");
    }

    private Loan loanReturnedOn(LocalDate checkoutDate) {
        return loans.findAll().stream().filter(l -> l.getCheckoutDate().equals(checkoutDate)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("AC-MEM-08 a loan returned 12 months ago is anonymised in the daily processing (seed S-15)")
    void anonymisation() {
        Loan before = loanReturnedOn(LocalDate.of(2025, 2, 24));
        assertThat(before.isAnonymised()).isFalse();
        fx.processThrough(LocalDate.of(2026, 3, 16));
        assertThat(loanReturnedOn(LocalDate.of(2025, 2, 24)).isAnonymised()).isFalse();
        fx.processThrough(LocalDate.of(2026, 3, 17));
        assertThat(loanReturnedOn(LocalDate.of(2025, 2, 24)).isAnonymised()).isTrue();
    }

    @Test
    @DisplayName("AC-MEM-09 a member who keeps the reading history is not anonymised (seed S-14)")
    void keepHistory() {
        Member noah = fx.member(14);
        fx.processThrough(LocalDate.of(2026, 3, 17));
        assertThat(loanQueries.historyOf(noah.getId())).hasSize(2);
        // withdrawing the opt-in makes the next daily processing anonymise the history
        memberService.updatePreferences(noah.getId(), new PreferencesRequest(noah.getEmail(), noah.getPhone(), false, false));
        fx.processThrough(LocalDate.of(2026, 3, 18));
        assertThat(loanQueries.historyOf(noah.getId())).isEmpty();
    }

    @Test
    @DisplayName("AC-MEM-10 a loan with an outstanding charge is not anonymised until the charge is settled (seed S-15)")
    void anonymisationWaitsForCharge() {
        Member olga = fx.member(15);
        fx.processThrough(LocalDate.of(2026, 3, 17));
        assertThat(loanReturnedOn(LocalDate.of(2025, 1, 6)).isAnonymised()).isFalse();
        assertThat(loanQueries.historyOf(olga.getId())).hasSize(1);

        payments.deskPayment(olga.getId(), new BigDecimal("1.00"), PaymentMethod.CASH, fx.librarianCen());
        fx.processThrough(LocalDate.of(2026, 3, 18));
        assertThat(loanReturnedOn(LocalDate.of(2025, 1, 6)).isAnonymised()).isTrue();
    }
}
