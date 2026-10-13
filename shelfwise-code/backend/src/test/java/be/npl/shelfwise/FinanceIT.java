package be.npl.shelfwise;

import static be.npl.shelfwise.support.Rules.assertForbidden;
import static be.npl.shelfwise.support.Rules.assertRefused;
import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.service.ChargeService;
import be.npl.shelfwise.finance.service.WaiverService;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.lending.service.LendingService;
import be.npl.shelfwise.lending.service.LoanQueryService;
import be.npl.shelfwise.lending.web.LendingDtos.LoanView;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.service.MemberStatusService;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.payment.entity.PaymentSessionStatus;
import be.npl.shelfwise.payment.repository.PaymentRepository;
import be.npl.shelfwise.payment.service.PaymentService;
import be.npl.shelfwise.payment.web.PaymentDtos.OnlinePaymentView;
import be.npl.shelfwise.payment.web.PaymentDtos.ProviderResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Appendix A.3 (accrued fines), A.4 (lost copies), A.5 (waivers) and A.6 (payments). */
class FinanceIT extends IntegrationTestBase {

    @Autowired LendingService lending;
    @Autowired LoanQueryService loanQueries;
    @Autowired WaiverService waivers;
    @Autowired PaymentService payments;
    @Autowired ChargeService chargeService;
    @Autowired MemberStatusService memberStatus;
    @Autowired LoanRepository loans;
    @Autowired PaymentRepository paymentRepository;

    private LoanView checkout(Member member, Copy copy) {
        return lending.checkout(member.getCardNumber(), copy.getBarcode(), fx.librarianCen()).loan();
    }

    private Charge chargeOfType(Member member, ChargeType type) {
        return fx.chargesOf(member).stream().filter(c -> c.getType() == type).findFirst().orElseThrow();
    }

    // ----- A.3 -----

    @Test
    @DisplayName("AC-FIN-01/02 fine charged on return: within grace period no charge, otherwise 0.25 per chargeable day")
    void fineOnReturn() {
        fx.today(LocalDate.of(2026, 2, 9));
        Member ada = fx.member(1);
        Copy onTime = fx.newCopy(MaterialType.BOOK, "CEN");
        Copy late = fx.newCopy(MaterialType.BOOK, "CEN");
        checkout(ada, onTime);
        checkout(ada, late);
        fx.today(LocalDate.of(2026, 3, 3)); // due 2026-03-02
        assertThat(lending.returnCopy(onTime.getBarcode(), fx.librarianCen()).fineCharged()).isEqualByComparingTo("0.00");
        assertThat(fx.chargesOf(ada)).isEmpty();
        fx.today(LocalDate.of(2026, 3, 4));
        assertThat(lending.returnCopy(late.getBarcode(), fx.librarianCen()).fineCharged()).isEqualByComparingTo("0.50");
        assertThat(fx.balance(ada)).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("AC-FIN-13 accrued fine is shown for an unreturned loan but creates no charge")
    void accruedFine() {
        fx.today(LocalDate.of(2026, 2, 9));
        Member ada = fx.member(1);
        checkout(ada, fx.newCopy(MaterialType.BOOK, "CEN"));
        fx.today(LocalDate.of(2026, 3, 4));
        LoanView view = loanQueries.openLoansOf(ada.getId()).get(0);
        assertThat(view.accruedFine()).isEqualByComparingTo("0.50");
        assertThat(fx.chargesOf(ada)).isEmpty();
        assertThat(fx.balance(ada)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("F-FIN-07 accrued fines of the seed on 2026-03-16 (S-08 senior, S-13 youth)")
    void seedAccruedFines() {
        assertThat(loanQueries.openLoansOf(fx.member(8).getId()).get(0).accruedFine()).isEqualByComparingTo("1.50");
        List<LoanView> mia = loanQueries.openLoansOf(fx.member(13).getId());
        assertThat(mia.stream().filter(l -> l.materialType() == MaterialType.BOOK).findFirst().orElseThrow().accruedFine())
                .isEqualByComparingTo("0.00");
        assertThat(mia.stream().filter(l -> l.materialType() == MaterialType.AUDIOVISUAL).findFirst().orElseThrow().accruedFine())
                .isEqualByComparingTo("6.00");
    }

    // ----- A.4 -----

    private Copy lostScenario() {
        fx.today(LocalDate.of(2026, 2, 9));
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        LoanView loan = checkout(fx.member(1), copy);
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        return copy;
    }

    @Test
    @DisplayName("AC-FIN-14/15 a loan is declared lost after 42 days with fine, replacement cost and fee; N-05 is sent")
    void lostDeclaration() {
        Member ada = fx.member(1);
        Copy copy = lostScenario();

        fx.processThrough(LocalDate.of(2026, 4, 12));
        assertThat(fx.reload(copy).getStatus()).isEqualTo(CopyStatus.ON_LOAN);
        assertThat(fx.chargesOf(ada)).isEmpty();

        fx.processThrough(LocalDate.of(2026, 4, 13));
        assertThat(fx.reload(copy).getStatus()).isEqualTo(CopyStatus.LOST);
        assertThat(loanQueries.openLoansOf(ada.getId())).isEmpty();
        assertThat(memberStatus.openLoanCount(ada.getId())).isZero();
        assertThat(loans.findByStatusAndLostDateIsNotNullOrderByLostDateDesc(LoanStatus.LOST)).isNotEmpty();
        assertThat(chargeOfType(ada, ChargeType.OVERDUE_FINE).getOriginalAmount()).isEqualByComparingTo("8.75");
        assertThat(chargeOfType(ada, ChargeType.LOST_REPLACEMENT).getOriginalAmount()).isEqualByComparingTo("25.00");
        assertThat(chargeOfType(ada, ChargeType.LOST_ADMIN_FEE).getOriginalAmount()).isEqualByComparingTo("5.00");
        assertThat(fx.balance(ada)).isEqualByComparingTo("38.75");
        assertThat(fx.notificationsOf(ada, "N-05")).hasSize(1);
    }

    @Test
    @DisplayName("AC-FIN-16 a lost copy returned within 90 days cancels the replacement cost and refunds what was paid")
    void lostCopyReturnedInWindow() {
        Member ada = fx.member(1);
        Copy copy = lostScenario();
        fx.processThrough(LocalDate.of(2026, 4, 13));
        payments.deskPayment(ada.getId(), new BigDecimal("38.75"), PaymentMethod.CASH, fx.librarianCen());
        assertThat(fx.balance(ada)).isEqualByComparingTo("0.00");

        fx.today(LocalDate.of(2026, 5, 20));
        var receipt = lending.returnCopy(copy.getBarcode(), fx.librarianCen());

        assertThat(receipt.lostCopy()).isTrue();
        assertThat(receipt.replacementCancelled()).isTrue();
        assertThat(chargeOfType(ada, ChargeType.LOST_REPLACEMENT).getStatus()).isEqualTo(ChargeStatus.CANCELLED);
        assertThat(chargeService.paidAmount(chargeOfType(ada, ChargeType.LOST_REPLACEMENT))).isEqualByComparingTo("0.00");
        assertThat(chargeOfType(ada, ChargeType.OVERDUE_FINE).getStatus()).isNotEqualTo(ChargeStatus.CANCELLED);
        assertThat(chargeOfType(ada, ChargeType.LOST_ADMIN_FEE).getStatus()).isNotEqualTo(ChargeStatus.CANCELLED);
        assertThat(fx.reload(copy).getStatus()).isNotEqualTo(CopyStatus.LOST);
    }

    @Test
    @DisplayName("AC-FIN-16 (unpaid) fine and fee remain due after the return of the lost copy")
    void lostCopyReturnedFineAndFeeRemain() {
        Member ada = fx.member(1);
        Copy copy = lostScenario();
        fx.processThrough(LocalDate.of(2026, 4, 13));
        fx.today(LocalDate.of(2026, 5, 20));
        lending.returnCopy(copy.getBarcode(), fx.librarianCen());
        assertThat(fx.balance(ada)).isEqualByComparingTo("13.75");
    }

    @Test
    @DisplayName("AC-FIN-17 a lost copy returned 91 days after the declaration cancels nothing; day 90 still does")
    void lostCopyReturnedAfterWindow() {
        Member ada = fx.member(1);
        Copy copy = lostScenario();
        fx.processThrough(LocalDate.of(2026, 4, 13));
        fx.today(LocalDate.of(2026, 7, 13));
        var receipt = lending.returnCopy(copy.getBarcode(), fx.librarianCen());
        assertThat(receipt.replacementCancelled()).isFalse();
        assertThat(chargeOfType(ada, ChargeType.LOST_REPLACEMENT).getStatus()).isEqualTo(ChargeStatus.OPEN);
        assertThat(fx.balance(ada)).isEqualByComparingTo("38.75");
        assertThat(fx.reload(copy).getStatus()).isNotEqualTo(CopyStatus.LOST);
    }

    @Test
    @DisplayName("AC-FIN-17 a return on 2026-07-12 (90 days after the declaration) still cancels the replacement cost")
    void lostCopyReturnedOnLastDayOfWindow() {
        Member ada = fx.member(1);
        Copy copy = lostScenario();
        fx.processThrough(LocalDate.of(2026, 4, 13));
        fx.today(LocalDate.of(2026, 7, 12));
        assertThat(lending.returnCopy(copy.getBarcode(), fx.librarianCen()).replacementCancelled()).isTrue();
        assertThat(chargeOfType(ada, ChargeType.LOST_REPLACEMENT).getStatus()).isEqualTo(ChargeStatus.CANCELLED);
    }

    @Test
    @DisplayName("Seed S-09: declared lost in the daily processing of 2026-03-17 (fine 9.00, replacement 25.00, fee 5.00)")
    void seedLostLoan() {
        Member iris = fx.member(9);
        fx.processThrough(LocalDate.of(2026, 3, 16));
        assertThat(fx.chargesOf(iris)).isEmpty();
        fx.processThrough(LocalDate.of(2026, 3, 17));
        assertThat(chargeOfType(iris, ChargeType.OVERDUE_FINE).getOriginalAmount()).isEqualByComparingTo("9.00");
        assertThat(chargeOfType(iris, ChargeType.LOST_REPLACEMENT).getOriginalAmount()).isEqualByComparingTo("25.00");
        assertThat(chargeOfType(iris, ChargeType.LOST_ADMIN_FEE).getOriginalAmount()).isEqualByComparingTo("5.00");
    }

    // ----- A.5 -----

    @Test
    @DisplayName("AC-FIN-18/19 a librarian waives up to 2.00 per charge, with a reason; more is refused")
    void librarianWaiverLimitPerCharge() {
        Charge charge = fx.addCharge(fx.member(1), ChargeType.OVERDUE_FINE, "3.00", LocalDate.of(2026, 3, 16));
        var view = waivers.waive(charge.getId(), new BigDecimal("2.00"), "Goodwill", fx.librarianCen());
        assertThat(view.outstandingAmount()).isEqualByComparingTo("1.00");
        assertRefused("LIBRARIAN_WAIVER_LIMIT",
                () -> waivers.waive(charge.getId(), new BigDecimal("0.50"), "More goodwill", fx.librarianCen()));
        assertThat(fx.charge(charge.getId()).getOutstandingAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("AC-FIN-20 a librarian cannot waive 2.01; a branch manager can")
    void managerWaiverLimit() {
        Charge charge = fx.addCharge(fx.member(1), ChargeType.OVERDUE_FINE, "3.00", LocalDate.of(2026, 3, 16));
        assertRefused("LIBRARIAN_WAIVER_LIMIT",
                () -> waivers.waive(charge.getId(), new BigDecimal("2.01"), "Too much", fx.librarianCen()));
        var view = waivers.waive(charge.getId(), new BigDecimal("2.01"), "Manager decision", fx.managerCen());
        assertThat(view.outstandingAmount()).isEqualByComparingTo("0.99");
    }

    @Test
    @DisplayName("AC-FIN-21 a waiver without a reason is refused")
    void waiverNeedsReason() {
        Charge charge = fx.addCharge(fx.member(1), ChargeType.OVERDUE_FINE, "3.00", LocalDate.of(2026, 3, 16));
        assertRefused("REASON_REQUIRED", () -> waivers.waive(charge.getId(), new BigDecimal("1.00"), "  ", fx.librarianCen()));
        assertRefused("REASON_REQUIRED", () -> waivers.waive(charge.getId(), new BigDecimal("1.00"), null, fx.managerCen()));
    }

    @Test
    @DisplayName("AC-FIN-22 staff cannot waive a charge on their own member account (seed S-19)")
    void ownAccountWaiver() {
        Charge sams = chargeOfType(fx.member(19), ChargeType.OVERDUE_FINE);
        assertRefused("OWN_ACCOUNT_WAIVER", () -> waivers.waive(sams.getId(), new BigDecimal("1.00"), "Mine", fx.librarianCen()));

        fx.linkStaffToMember("manager.cen", fx.member(1));
        Charge adas = fx.addCharge(fx.member(1), ChargeType.OVERDUE_FINE, "3.00", LocalDate.of(2026, 3, 16));
        assertRefused("OWN_ACCOUNT_WAIVER", () -> waivers.waive(adas.getId(), new BigDecimal("1.00"), "Mine", fx.managerCen()));
    }

    @Test
    @DisplayName("AC-FIN-23 an administrator cannot waive a charge")
    void administratorCannotWaive() {
        Charge charge = fx.addCharge(fx.member(1), ChargeType.OVERDUE_FINE, "3.00", LocalDate.of(2026, 3, 16));
        assertForbidden(() -> waivers.waive(charge.getId(), new BigDecimal("1.00"), "Admin", fx.admin()));
    }

    // ----- A.6 -----

    @Test
    @DisplayName("AC-PAY-01 a desk payment is allocated to the oldest charges first (seed S-18)")
    void deskPaymentAllocation() {
        Member rosa = fx.member(18);
        payments.deskPayment(rosa.getId(), new BigDecimal("3.50"), PaymentMethod.CASH, fx.librarianCen());

        List<Charge> charges = fx.chargesOf(rosa); // newest first: C, B, A
        Charge c = charges.get(0);
        Charge b = charges.get(1);
        Charge a = charges.get(2);
        assertThat(a.getStatus()).isEqualTo(ChargeStatus.SETTLED);
        assertThat(a.getOutstandingAmount()).isEqualByComparingTo("0.00");
        assertThat(b.getStatus()).isEqualTo(ChargeStatus.OPEN);
        assertThat(b.getOutstandingAmount()).isEqualByComparingTo("0.50");
        assertThat(c.getOutstandingAmount()).isEqualByComparingTo("2.00");
        assertThat(fx.balance(rosa)).isEqualByComparingTo("2.50");
        assertThat(memberStatus.blockStatus(fx.reload(rosa), LocalDate.of(2026, 3, 16)).blocked()).isFalse();
        assertThat(fx.notificationsOf(rosa, "N-10")).hasSize(1);
    }

    @Test
    @DisplayName("AC-PAY-02 a payment above the balance is refused (seed S-05)")
    void paymentExceedsBalance() {
        Member eva = fx.member(5);
        assertRefused("PAYMENT_EXCEEDS_BALANCE",
                () -> payments.deskPayment(eva.getId(), new BigDecimal("6.00"), PaymentMethod.CASH, fx.librarianCen()));
        assertThat(fx.balance(eva)).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("AC-PAY-03 an online payment of one charge settles only that charge and sends N-10 (seed S-18)")
    void onlinePaymentOfOneCharge() {
        Member rosa = fx.member(18);
        List<Charge> charges = fx.chargesOf(rosa);
        Charge c = charges.get(0);
        OnlinePaymentView started = payments.startOnlinePayment(rosa.getId(), List.of(c.getId()));
        assertThat(started.amount()).isEqualByComparingTo("2.00");

        OnlinePaymentView done = payments.completeOnlinePayment(started.reference(), ProviderResult.SUCCEEDS);

        assertThat(done.status()).isEqualTo(PaymentSessionStatus.SUCCEEDED);
        assertThat(fx.charge(c.getId()).getStatus()).isEqualTo(ChargeStatus.SETTLED);
        assertThat(fx.charge(charges.get(1).getId()).getOutstandingAmount()).isEqualByComparingTo("3.00");
        assertThat(fx.charge(charges.get(2).getId()).getOutstandingAmount()).isEqualByComparingTo("1.00");
        assertThat(fx.notificationsOf(rosa, "N-10")).hasSize(1);
    }

    @Test
    @DisplayName("AC-PAY-04 a failed or cancelled online payment changes nothing and sends no N-10")
    void onlinePaymentFailsOrIsCancelled() {
        Member rosa = fx.member(18);
        for (ProviderResult result : List.of(ProviderResult.FAILS, ProviderResult.CANCELLED)) {
            OnlinePaymentView started = payments.startOnlinePayment(rosa.getId(), List.of());
            OnlinePaymentView done = payments.completeOnlinePayment(started.reference(), result);
            assertThat(done.status()).isIn(PaymentSessionStatus.FAILED, PaymentSessionStatus.CANCELLED);
            assertThat(done.message()).isNotBlank();
        }
        assertThat(fx.balance(rosa)).isEqualByComparingTo("6.00");
        assertThat(fx.notificationsOf(rosa, "N-10")).isEmpty();
        assertThat(paymentRepository.findByMemberIdOrderByIdDesc(rosa.getId())).isEmpty();
    }

    @Test
    @DisplayName("AC-PAY-05 the same successful online payment reported twice is processed once")
    void onlinePaymentIsIdempotent() {
        Member rosa = fx.member(18);
        OnlinePaymentView started = payments.startOnlinePayment(rosa.getId(), List.of());
        payments.completeOnlinePayment(started.reference(), ProviderResult.SUCCEEDS);
        payments.completeOnlinePayment(started.reference(), ProviderResult.SUCCEEDS);

        assertThat(fx.balance(rosa)).isEqualByComparingTo("0.00");
        assertThat(paymentRepository.findByMemberIdOrderByIdDesc(rosa.getId())).hasSize(1);
        assertThat(fx.notificationsOf(rosa, "N-10")).hasSize(1);
    }
}
