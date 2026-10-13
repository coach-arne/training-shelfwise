package be.npl.shelfwise;

import static be.npl.shelfwise.support.Rules.assertRefused;
import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.catalogue.service.CatalogueService;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.notification.entity.Notification;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.service.ReservationService;
import be.npl.shelfwise.reservation.web.ReservationDtos.ReservationView;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Appendix A.7: reservations. */
class ReservationIT extends IntegrationTestBase {

    @Autowired ReservationService reservations;
    @Autowired LendingService lending;
    @Autowired CatalogueService catalogue;

    /** A title with a single copy that is on loan to member S-06, so that it can be reserved. */
    private record OnLoan(Title title, Copy copy) {
    }

    private OnLoan titleOnLoan() {
        Title title = fx.newTitle(MaterialType.BOOK);
        Copy copy = fx.newCopyOf(title, "CEN");
        lending.checkout(fx.member(6).getCardNumber(), copy.getBarcode(), fx.librarianCen());
        return new OnLoan(title, copy);
    }

    private ReservationView reserve(Member member, Title title) {
        return reservations.place(member.getId(), title.getId(), fx.branch("CEN").getId());
    }

    private Charge feeOf(Member member) {
        return fx.chargesOf(member).stream().filter(c -> c.getType() == ChargeType.RESERVATION_FEE).findFirst().orElseThrow();
    }

    private ReservationView latest(Member member) {
        return reservations.listForMember(member.getId()).get(0);
    }

    @Test
    @DisplayName("AC-RES-01 an ADULT pays a reservation fee of 0.50")
    void adultFee() {
        Member ada = fx.member(1);
        reserve(ada, titleOnLoan().title());
        assertThat(feeOf(ada).getOriginalAmount()).isEqualByComparingTo("0.50");
        assertThat(fx.balance(ada)).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("AC-RES-02 a YOUTH pays no reservation fee (seed S-03)")
    void youthNoFee() {
        Member cleo = fx.member(3);
        reserve(cleo, titleOnLoan().title());
        assertThat(fx.chargesOf(cleo)).isEmpty();
    }

    @Test
    @DisplayName("AC-RES-03 a sixth active reservation is refused (seed S-12)")
    void reservationLimit() {
        Member leo = fx.member(12);
        assertThat(reservations.listForMember(leo.getId())).hasSize(5);
        Title title = titleOnLoan().title();
        assertRefused("RESERVATION_LIMIT_REACHED", () -> reserve(leo, title));
    }

    @Test
    @DisplayName("AC-RES-04 reserving a title with an available copy at the pickup branch is refused")
    void availableCopyAtPickupBranch() {
        Title title = fx.newTitle(MaterialType.BOOK);
        fx.newCopyOf(title, "CEN");
        assertRefused("COPY_AVAILABLE_AT_BRANCH", () -> reserve(fx.member(1), title));
        // an available copy elsewhere does not prevent a reservation for another pickup branch
        assertThat(reservations.place(fx.member(1).getId(), title.getId(), fx.branch("RIV").getId()).status())
                .isEqualTo(ReservationStatus.WAITING);
    }

    @Test
    @DisplayName("AC-RES-05 reserving a title of which the member has a copy on loan is refused")
    void titleOnLoanToMember() {
        OnLoan onLoan = titleOnLoan();
        assertRefused("TITLE_ON_LOAN_TO_MEMBER", () -> reserve(fx.member(6), onLoan.title()));
    }

    @Test
    @DisplayName("AC-RES-06 the copy goes on hold for the first member in the queue; N-06 is sent to them only")
    void queueOrder() {
        OnLoan onLoan = titleOnLoan();
        Member x = fx.member(1);
        Member y = fx.member(4);
        reserve(x, onLoan.title());
        reserve(y, onLoan.title());

        lending.returnCopy(onLoan.copy().getBarcode(), fx.librarianCen());

        assertThat(fx.reload(onLoan.copy()).getStatus()).isEqualTo(CopyStatus.ON_HOLD);
        assertThat(latest(x).status()).isEqualTo(ReservationStatus.ON_HOLD);
        assertThat(latest(y).status()).isEqualTo(ReservationStatus.WAITING);
        assertThat(fx.notificationsOf(x, "N-06")).hasSize(1);
        assertThat(fx.notificationsOf(y, "N-06")).isEmpty();
    }

    @Test
    @DisplayName("AC-RES-07 reminder on day 5, expiry on the last hold day; the copy goes on hold for the next member")
    void holdExpiry() {
        fx.today(LocalDate.of(2026, 3, 2));
        OnLoan onLoan = titleOnLoan();
        Member x = fx.member(1);
        Member y = fx.member(4);
        reserve(x, onLoan.title());
        reserve(y, onLoan.title());
        lending.returnCopy(onLoan.copy().getBarcode(), fx.librarianCen());
        assertThat(latest(x).holdUntil()).isEqualTo(LocalDate.of(2026, 3, 9));

        fx.processThrough(LocalDate.of(2026, 3, 6));
        assertThat(fx.notificationsOf(x, "N-07")).isEmpty();
        fx.processThrough(LocalDate.of(2026, 3, 7));
        assertThat(fx.notificationsOf(x, "N-07")).extracting(Notification::getNotificationDate)
                .containsExactly(LocalDate.of(2026, 3, 7));

        fx.processThrough(LocalDate.of(2026, 3, 8));
        assertThat(latest(x).status()).isEqualTo(ReservationStatus.ON_HOLD);

        fx.processThrough(LocalDate.of(2026, 3, 9));
        assertThat(latest(x).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(fx.notificationsOf(x, "N-08")).hasSize(1);
        assertThat(latest(y).status()).isEqualTo(ReservationStatus.ON_HOLD);
        assertThat(fx.reload(onLoan.copy()).getStatus()).isEqualTo(CopyStatus.ON_HOLD);
        assertThat(fx.notificationsOf(y, "N-06")).hasSize(1);
    }

    @Test
    @DisplayName("AC-RES-08 a hold notified on Thu 2026-05-07 lasts until Fri 2026-05-15 (Ascension Day); reminder on 2026-05-12")
    void holdPeriodOverHoliday() {
        fx.today(LocalDate.of(2026, 5, 7));
        OnLoan onLoan = titleOnLoan();
        Member x = fx.member(1);
        reserve(x, onLoan.title());
        lending.returnCopy(onLoan.copy().getBarcode(), fx.librarianCen());
        assertThat(latest(x).holdUntil()).isEqualTo(LocalDate.of(2026, 5, 15));
        fx.processThrough(LocalDate.of(2026, 5, 12));
        assertThat(fx.notificationsOf(x, "N-07")).extracting(Notification::getNotificationDate)
                .containsExactly(LocalDate.of(2026, 5, 12));
        fx.processThrough(LocalDate.of(2026, 5, 14));
        assertThat(latest(x).status()).isEqualTo(ReservationStatus.ON_HOLD);
        fx.processThrough(LocalDate.of(2026, 5, 15));
        assertThat(latest(x).status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("AC-RES-09 a cancelled reservation is not refunded")
    void cancellationKeepsFee() {
        Member ada = fx.member(1);
        ReservationView view = reserve(ada, titleOnLoan().title());
        ReservationView cancelled = reservations.cancel(view.id(), fx.asMember(ada));
        assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(feeOf(ada).getStatus()).isEqualTo(ChargeStatus.OPEN);
        assertThat(fx.balance(ada)).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("AC-RES-10 withdrawing the last copy cancels all waiting reservations and their fees")
    void withdrawLastCopy() {
        Title title = fx.newTitle(MaterialType.BOOK);
        Copy copy = fx.newCopyOf(title, "CEN");
        catalogue.changeStatus(copy.getId(), CopyStatus.DAMAGED);
        Member x = fx.member(1);
        Member y = fx.member(4);
        reserve(x, title);
        reserve(y, title);

        catalogue.changeStatus(copy.getId(), CopyStatus.WITHDRAWN);

        for (Member member : List.of(x, y)) {
            ReservationView view = latest(member);
            assertThat(view.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(view.cancelledByLibrary()).isTrue();
            assertThat(feeOf(member).getStatus()).isEqualTo(ChargeStatus.CANCELLED);
            assertThat(fx.balance(member)).isEqualByComparingTo("0.00");
        }
    }

    @Test
    @DisplayName("Seed S-16/S-17: Pia's hold expires in the daily processing of 2026-03-16 and the copy goes to Quinn")
    void seedHoldExpiry() {
        fx.processThrough(LocalDate.of(2026, 3, 16));
        assertThat(latest(fx.member(16)).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(latest(fx.member(17)).status()).isEqualTo(ReservationStatus.ON_HOLD);
        assertThat(fx.notificationsOf(fx.member(16), "N-08")).hasSize(1);
        assertThat(fx.notificationsOf(fx.member(17), "N-06")).hasSize(1);
    }

    @Test
    @DisplayName("F-RES-03 the queue position follows the order of placement")
    void queuePosition() {
        OnLoan onLoan = titleOnLoan();
        reserve(fx.member(1), onLoan.title());
        ReservationView second = reserve(fx.member(4), onLoan.title());
        assertThat(second.queuePosition()).isEqualTo(2);
    }
}
