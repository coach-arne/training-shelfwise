package be.npl.shelfwise;

import static be.npl.shelfwise.support.Rules.assertRefused;
import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.lending.helper.CopyRouter.RoutingOutcome;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.lending.service.LoanQueryService;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.reservation.service.ReservationService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Appendix A.1: due dates, renewals and routing. */
class LendingIT extends IntegrationTestBase {

    @Autowired LendingService lending;
    @Autowired LoanQueryService loanQueries;
    @Autowired ReservationService reservations;

    private LoanView checkout(Member member, Copy copy) {
        return lending.checkout(member.getCardNumber(), copy.getBarcode(), fx.librarianCen()).loan();
    }

    @Test
    @DisplayName("AC-LEN-01 BOOK checked out on Mon 2026-03-02 is due Mon 2026-03-23")
    void bookDueDate() {
        fx.today(LocalDate.of(2026, 3, 2));
        LoanView loan = checkout(fx.member(1), fx.newCopy(MaterialType.BOOK, "CEN"));
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 23));
    }

    @Test
    @DisplayName("AC-LEN-02 due date on Easter Monday moves to Tuesday 2026-04-07")
    void easterMonday() {
        fx.today(LocalDate.of(2026, 3, 16));
        LoanView loan = checkout(fx.member(1), fx.newCopy(MaterialType.BOOK, "CEN"));
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 4, 7));
    }

    @Test
    @DisplayName("AC-LEN-03 AUDIOVISUAL due on Ascension Day moves to Friday 2026-05-15")
    void ascension() {
        fx.today(LocalDate.of(2026, 5, 7));
        LoanView loan = checkout(fx.member(1), fx.newCopy(MaterialType.AUDIOVISUAL, "CEN"));
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("AC-LEN-04/05/06 renewals extend from the current due date; the third attempt is refused")
    void renewals() {
        fx.today(LocalDate.of(2026, 3, 2));
        LoanView loan = checkout(fx.member(1), fx.newCopy(MaterialType.BOOK, "CEN"));
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 23));

        fx.today(LocalDate.of(2026, 3, 10));
        assertThat(lending.renew(loan.id(), fx.librarianCen()).dueDate()).isEqualTo(LocalDate.of(2026, 4, 13));

        fx.today(LocalDate.of(2026, 4, 1));
        assertThat(lending.renew(loan.id(), fx.librarianCen()).dueDate()).isEqualTo(LocalDate.of(2026, 5, 4));

        assertRefused("MAX_RENEWALS_REACHED", () -> lending.renew(loan.id(), fx.librarianCen()));
    }

    @Test
    @DisplayName("AC-LEN-07 a renewal that ends on Easter Monday moves to Tuesday 2026-04-07")
    void renewalEasterMonday() {
        fx.today(LocalDate.of(2026, 2, 23));
        LoanView loan = checkout(fx.member(1), fx.newCopy(MaterialType.BOOK, "CEN"));
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 16));
        fx.today(LocalDate.of(2026, 3, 12));
        assertThat(lending.renew(loan.id(), fx.librarianCen()).dueDate()).isEqualTo(LocalDate.of(2026, 4, 7));
    }

    @Test
    @DisplayName("AC-LEN-08 renewal refused while another member has a waiting reservation (seed S-11, S-12)")
    void renewalRefusedWithReservation() {
        Member kim = fx.member(11);
        LoanView atlas = loanQueries.openLoansOf(kim.getId()).get(0);
        assertThat(atlas.title()).contains("Lighthouse");
        assertRefused("RENEWAL_RESERVATIONS_WAITING", () -> lending.renew(atlas.id(), fx.librarianCen()));
    }

    @Test
    @DisplayName("AC-LEN-09 renewal of an overdue loan is refused (seed S-08)")
    void renewalRefusedWhenOverdue() {
        LoanView overdue = loanQueries.openLoansOf(fx.member(8).getId()).get(0);
        assertRefused("LOAN_OVERDUE", () -> lending.renew(overdue.id(), fx.librarianCen()));
    }

    @Test
    @DisplayName("AC-LEN-10 renewal by a member with balance 5.00 is refused")
    void renewalRefusedWhenBlocked() {
        fx.today(LocalDate.of(2026, 3, 2));
        Member ada = fx.member(1);
        LoanView loan = checkout(ada, fx.newCopy(MaterialType.BOOK, "CEN"));
        fx.addCharge(ada, ChargeType.OVERDUE_FINE, "5.00", LocalDate.of(2026, 3, 2));
        assertRefused("MEMBER_BLOCKED", () -> lending.renew(loan.id(), fx.librarianCen()));
        assertRefused("MEMBER_BLOCKED", () -> lending.renew(loan.id(), fx.asMember(ada)));
    }

    @Test
    @DisplayName("AC-LEN-12 a copy returned away from its home branch goes in transit and is available after check-in")
    void transitToHome() {
        Member ada = fx.member(1);
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        checkout(ada, copy);

        var receipt = lending.returnCopy(copy.getBarcode(), fx.librarianRiv());
        assertThat(receipt.routing().outcome()).isEqualTo(RoutingOutcome.TRANSIT_TO_HOME);
        assertThat(fx.reload(copy).getStatus()).isEqualTo(CopyStatus.IN_TRANSIT);

        var arrival = lending.arrive(copy.getBarcode(), fx.librarianCen());
        assertThat(arrival.routing().copyStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(fx.reload(copy).getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("AC-LEN-13 a copy returned at the pickup branch of the first reservation is put on hold; N-06 is sent")
    void holdForFirstReservation() {
        Title title = fx.newTitle(MaterialType.BOOK);
        Copy copy = fx.newCopyOf(title, "CEN");
        checkout(fx.member(1), copy);
        Member dan = fx.member(4);
        reservations.place(dan.getId(), title.getId(), fx.branch("CEN").getId());

        var receipt = lending.returnCopy(copy.getBarcode(), fx.librarianCen());

        assertThat(receipt.routing().outcome()).isEqualTo(RoutingOutcome.HOLD_HERE);
        assertThat(fx.reload(copy).getStatus()).isEqualTo(CopyStatus.ON_HOLD);
        assertThat(fx.notificationsOf(dan, "N-06")).isNotEmpty();
    }

    @Test
    @DisplayName("F-LEN-05 returned copy: a fine is charged and the loan is closed on the return date")
    void returnWithFine() {
        fx.today(LocalDate.of(2026, 3, 2));
        Member ada = fx.member(1);
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        checkout(ada, copy);
        fx.today(LocalDate.of(2026, 3, 23 + 3));
        var receipt = lending.returnCopy(copy.getBarcode(), fx.librarianCen());
        // due 03-23, returned Thu 03-26: 3 chargeable days x 0.25
        assertThat(receipt.chargeableDays()).isEqualTo(3);
        assertThat(receipt.fineCharged()).isEqualByComparingTo("0.75");
        assertThat(fx.balance(ada)).isEqualByComparingTo("0.75");
    }
}
