package be.npl.shelfwise.seed;

import be.npl.shelfwise.catalogue.entity.Branch;
import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.CopyStatus;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.catalogue.entity.Title;
import be.npl.shelfwise.config.AppProperties;
import be.npl.shelfwise.finance.entity.Charge;
import be.npl.shelfwise.finance.entity.ChargeStatus;
import be.npl.shelfwise.finance.entity.ChargeTransaction;
import be.npl.shelfwise.finance.entity.ChargeType;
import be.npl.shelfwise.finance.entity.TransactionType;
import be.npl.shelfwise.finance.repository.ChargeRepository;
import be.npl.shelfwise.finance.repository.ChargeTransactionRepository;
import be.npl.shelfwise.lending.entity.Channel;
import be.npl.shelfwise.lending.entity.Loan;
import be.npl.shelfwise.lending.entity.LoanEvent;
import be.npl.shelfwise.lending.entity.LoanEventType;
import be.npl.shelfwise.lending.entity.LoanStatus;
import be.npl.shelfwise.lending.repository.LoanEventRepository;
import be.npl.shelfwise.lending.repository.LoanRepository;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.member.helper.CardNumbers;
import be.npl.shelfwise.member.repository.MemberRepository;
import be.npl.shelfwise.payment.entity.Payment;
import be.npl.shelfwise.payment.entity.PaymentAllocation;
import be.npl.shelfwise.payment.entity.PaymentMethod;
import be.npl.shelfwise.payment.repository.PaymentAllocationRepository;
import be.npl.shelfwise.payment.repository.PaymentRepository;
import be.npl.shelfwise.reservation.entity.Reservation;
import be.npl.shelfwise.reservation.entity.ReservationStatus;
import be.npl.shelfwise.reservation.repository.ReservationRepository;
import be.npl.shelfwise.security.Role;
import be.npl.shelfwise.security.StaffAccount;
import be.npl.shelfwise.security.StaffAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds members S-01 to S-19, their loans, charges and reservations, and the staff accounts (Appendix B). */
@Component
@RequiredArgsConstructor
public class SeedScenarioLoader {

    public static final String DEMO_PASSWORD = "shelfwise";

    private static final LocalDate MEMBERSHIP_START = LocalDate.of(2025, 10, 1);
    private static final LocalDate MEMBERSHIP_END = LocalDate.of(2026, 9, 30);

    private final MemberRepository members;
    private final StaffAccountRepository staffAccounts;
    private final LoanRepository loans;
    private final LoanEventRepository loanEvents;
    private final ReservationRepository reservations;
    private final ChargeRepository charges;
    private final ChargeTransactionRepository chargeTransactions;
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    private record Ctx(String passwordHash, SeedCatalogue catalogue, Branch central, Branch riverside) {
    }

    public void load(SeedCatalogue catalogue) {
        Ctx ctx = new Ctx(passwordEncoder.encode(DEMO_PASSWORD), catalogue, catalogue.branches().get("CEN"),
                catalogue.branches().get("RIV"));

        Member ada = member(ctx, "Ada", "Adams", "1988-04-12", MemberCategory.ADULT);
        Member ben = member(ctx, "Ben", "Brooks", "1979-09-03", MemberCategory.ADULT);
        Member cleo = member(ctx, "Cleo", "Carter", "2014-05-20", MemberCategory.YOUTH);
        Member dan = member(ctx, "Dan", "Carter", "1980-01-30", MemberCategory.ADULT);
        Member eva = member(ctx, "Eva", "Evans", "1991-07-22", MemberCategory.ADULT);
        Member finn = member(ctx, "Finn", "Foster", "1984-12-05", MemberCategory.ADULT);
        Member gina = member(ctx, "Gina", "Gray", "1975-03-18", MemberCategory.ADULT);
        Member hugo = member(ctx, "Hugo", "Hale", "1952-11-02", MemberCategory.SENIOR);
        Member iris = member(ctx, "Iris", "Irwin", "1993-06-30", MemberCategory.ADULT);
        Member jack = member(ctx, "Jack", "Jones", "1969-02-14", MemberCategory.ADULT);
        Member kim = member(ctx, "Kim", "King", "1990-10-09", MemberCategory.ADULT);
        Member leo = member(ctx, "Leo", "Lane", "1986-08-27", MemberCategory.ADULT);
        Member mia = member(ctx, "Mia", "Moore", "2012-08-14", MemberCategory.YOUTH);
        Member noah = member(ctx, "Noah", "Nash", "1982-05-11", MemberCategory.ADULT);
        Member olga = member(ctx, "Olga", "Owen", "1977-09-25", MemberCategory.ADULT);
        Member pia = member(ctx, "Pia", "Price", "1995-01-19", MemberCategory.ADULT);
        Member quinn = member(ctx, "Quinn", "Quill", "1989-04-04", MemberCategory.ADULT);
        Member rosa = member(ctx, "Rosa", "Reed", "1973-12-21", MemberCategory.ADULT);
        Member sam = member(ctx, "Sam", "Stone", "1981-06-06", MemberCategory.ADULT);

        // Guardians (S-03 and S-13 have Dan Carter as guardian).
        cleo.setGuardianMember(dan);
        mia.setGuardianMember(dan);
        members.save(cleo);
        members.save(mia);

        // S-07: membership valid until 2026-03-10.
        gina.setMembershipStart(LocalDate.of(2025, 3, 11));
        gina.setMembershipEnd(LocalDate.of(2026, 3, 10));
        members.save(gina);

        // S-14: keeps the reading history.
        noah.setKeepHistory(true);
        members.save(noah);

        seedBenAndCleo(ctx, ben, cleo);
        seedChargesOfSettledLoans(ctx, eva, finn, rosa, sam);
        seedOverdueLoans(ctx, hugo, iris, jack, mia);
        seedKimAndLeo(ctx, kim, leo);
        seedPrivacyScenarios(ctx, noah, olga);
        seedHoldScenario(ctx, pia, quinn);
        // Ada has no loans and no charges (S-01).

        seedStaff(ctx, sam);
    }

    // ----- members, staff -----

    private Member member(Ctx ctx, String first, String last, String birth, MemberCategory category) {
        Member m = new Member();
        m.setCardNumber(CardNumbers.format(members.nextCardSequence()));
        m.setFirstName(first);
        m.setLastName(last);
        m.setBirthDate(LocalDate.parse(birth));
        m.setStreet("Elm Street " + (first.charAt(0) - 'A' + 1));
        m.setPostalCode("1000");
        m.setCity("Northwick");
        m.setEmail((first + "." + last).toLowerCase() + "@example.org");
        m.setPhone("+32 470 00 00 " + String.format("%02d", (int) members.count() + 1));
        m.setCategory(category);
        m.setRegistrationDate(MEMBERSHIP_START);
        m.setMembershipStart(MEMBERSHIP_START);
        m.setMembershipEnd(MEMBERSHIP_END);
        m.setPasswordHash(ctx.passwordHash());
        return members.save(m);
    }

    private void seedStaff(Ctx ctx, Member sam) {
        staff(ctx, "admin", "Alex Admin", Role.ADMINISTRATOR, null, null);
        staff(ctx, "manager.cen", "Morgan Manager", Role.BRANCH_MANAGER, ctx.central(), null);
        staff(ctx, "librarian.cen", "Sam Stone", Role.LIBRARIAN, ctx.central(), sam);
        staff(ctx, "librarian.riv", "Robin Rivers", Role.LIBRARIAN, ctx.riverside(), null);
    }

    private void staff(Ctx ctx, String username, String name, Role role, Branch branch, Member linked) {
        StaffAccount account = new StaffAccount();
        account.setUsername(username);
        account.setDisplayName(name);
        account.setPasswordHash(ctx.passwordHash());
        account.setRole(role);
        account.setBranch(branch);
        account.setLinkedMember(linked);
        account.setActive(true);
        staffAccounts.save(account);
    }

    // ----- scenarios -----

    /** S-02 (10 open loans, 4 of them the single copies Leo reserved) and S-03 (5 open loans). */
    private void seedBenAndCleo(Ctx ctx, Member ben, Member cleo) {
        LocalDate checkout = LocalDate.of(2026, 3, 9);
        LocalDate due = LocalDate.of(2026, 3, 30);
        ctx.catalogue().reservedCopies().forEach(copy -> openLoan(ben, copy, checkout, due, 0, ctx.central()));
        for (int i = 0; i < 6; i++) {
            openLoan(ben, ctx.catalogue().takeCopy(MaterialType.BOOK), checkout, due, 0, ctx.central());
        }
        for (int i = 0; i < 5; i++) {
            openLoan(cleo, ctx.catalogue().takeCopy(MaterialType.BOOK), checkout, due, 0, ctx.riverside());
        }
    }

    /** S-05, S-06, S-18, S-19: outstanding overdue fines on returned loans. */
    private void seedChargesOfSettledLoans(Ctx ctx, Member eva, Member finn, Member rosa, Member sam) {
        chargeOnReturnedLoan(ctx, eva, "2026-02-02", "2026-02-23", "2026-03-10", "5.00", "5.00");
        chargeOnReturnedLoan(ctx, finn, "2026-02-09", "2026-03-02", "2026-03-12", "5.25", "4.99");
        chargeOnReturnedLoan(ctx, rosa, "2026-02-02", "2026-02-23", "2026-03-01", "1.00", "1.00");
        chargeOnReturnedLoan(ctx, rosa, "2026-02-09", "2026-03-02", "2026-03-10", "3.00", "3.00");
        chargeOnReturnedLoan(ctx, rosa, "2026-02-16", "2026-03-09", "2026-03-14", "2.00", "2.00");
        chargeOnReturnedLoan(ctx, sam, "2026-02-12", "2026-03-05", "2026-03-13", "1.50", "1.50");
    }

    /** S-08, S-09, S-10, S-13: overdue and renewed loans. */
    private void seedOverdueLoans(Ctx ctx, Member hugo, Member iris, Member jack, Member mia) {
        SeedCatalogue cat = ctx.catalogue();
        openLoan(hugo, cat.takeCopy(MaterialType.BOOK), date("2026-02-09"), date("2026-03-02"), 0, ctx.riverside());
        openLoan(iris, cat.takeCopy(MaterialType.BOOK), date("2026-01-13"), date("2026-02-03"), 0, ctx.central());
        openLoan(jack, cat.takeCopy(MaterialType.BOOK), date("2026-01-19"), date("2026-03-23"), 2, ctx.riverside());
        openLoan(mia, cat.takeCopy(MaterialType.BOOK), date("2026-02-09"), date("2026-03-02"), 0, ctx.central());
        openLoan(mia, cat.takeCopy(MaterialType.AUDIOVISUAL), date("2026-02-23"), date("2026-03-02"), 0, ctx.central());
    }

    /** S-11 has the only copy of the Atlas; S-12 has five waiting reservations (limit reached). */
    private void seedKimAndLeo(Ctx ctx, Member kim, Member leo) {
        SeedCatalogue cat = ctx.catalogue();
        openLoan(kim, cat.lighthouseCopy(), date("2026-03-09"), date("2026-03-30"), 0, ctx.central());

        reserveWithFee(leo, cat.lighthouseTitle(), ctx.central(), date("2026-03-10"));
        for (int i = 0; i < cat.reservedTitles().size(); i++) {
            reserveWithFee(leo, cat.reservedTitles().get(i), ctx.central(), date("2026-03-11").plusDays(i / 2));
        }
    }

    /** S-14 and S-15: returned loans for the anonymisation scenarios. */
    private void seedPrivacyScenarios(Ctx ctx, Member noah, Member olga) {
        returnedLoan(ctx, noah, "2024-12-26", "2025-01-16", "2025-01-15");
        returnedLoan(ctx, noah, "2025-01-20", "2025-02-10", "2025-02-10");
        returnedLoan(ctx, olga, "2025-02-24", "2025-03-17", "2025-03-17");
        Loan withFine = returnedLoan(ctx, olga, "2025-01-06", "2025-01-27", "2025-02-03");
        charge(olga, ChargeType.OVERDUE_FINE, "1.00", "1.00", date("2025-02-03"), withFine, null, "Overdue fine");
    }

    /** S-16 has a copy on hold that expires on the seed date; S-17 is next in the queue. */
    private void seedHoldScenario(Ctx ctx, Member pia, Member quinn) {
        SeedCatalogue cat = ctx.catalogue();
        Reservation hold = reservation(cat.holdTitle(), pia, ctx.central(), ReservationStatus.ON_HOLD,
                placedAt(date("2026-03-02"), 10));
        hold.setAssignedCopy(cat.holdCopy());
        hold.setNotifiedOn(date("2026-03-09"));
        hold.setHoldUntil(date("2026-03-16"));
        hold.setReminderSent(true);
        reservations.save(hold);

        reservation(cat.holdTitle(), quinn, ctx.central(), ReservationStatus.WAITING, placedAt(date("2026-03-05"), 14));
    }

    // ----- building blocks -----

    private Loan openLoan(Member member, Copy copy, LocalDate checkout, LocalDate due, int renewals, Branch branch) {
        copy.setStatus(CopyStatus.ON_LOAN);
        Loan loan = baseLoan(member, copy, checkout, due, branch);
        loan.setStatus(LoanStatus.OPEN);
        loan.setRenewalCount(renewals);
        loans.save(loan);
        event(loan, LoanEventType.CHECKOUT, checkout, branch);
        return loan;
    }

    private Loan returnedLoan(Ctx ctx, Member member, String checkout, String due, String returned) {
        Copy copy = ctx.catalogue().historicCopy(MaterialType.BOOK);
        Loan loan = baseLoan(member, copy, date(checkout), date(due), ctx.central());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(date(returned));
        loan.setReturnBranch(ctx.central());
        loans.save(loan);
        event(loan, LoanEventType.CHECKOUT, date(checkout), ctx.central());
        event(loan, LoanEventType.RETURN, date(returned), ctx.central());
        return loan;
    }

    private void chargeOnReturnedLoan(Ctx ctx, Member member, String checkout, String due, String returned,
                                      String original, String outstanding) {
        Loan loan = returnedLoan(ctx, member, checkout, due, returned);
        charge(member, ChargeType.OVERDUE_FINE, original, outstanding, date(returned), loan, null, "Overdue fine");
    }

    private Loan baseLoan(Member member, Copy copy, LocalDate checkout, LocalDate due, Branch branch) {
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setMember(member);
        loan.setMemberCategory(member.getCategory());
        loan.setCheckoutDate(checkout);
        loan.setDueDate(due);
        loan.setCheckoutBranch(branch);
        loan.setRenewalCount(0);
        loan.setAnonymised(false);
        return loan;
    }

    private void event(Loan loan, LoanEventType type, LocalDate date, Branch branch) {
        LoanEvent event = new LoanEvent();
        event.setLoan(loan);
        event.setType(type);
        event.setEventDate(date);
        event.setBranch(branch);
        event.setChannel(Channel.DESK);
        event.setMaterialType(loan.getCopy().getTitle().getMaterialType());
        loanEvents.save(event);
    }

    private Reservation reservation(Title title, Member member, Branch pickup, ReservationStatus status, Instant placed) {
        Reservation reservation = new Reservation();
        reservation.setTitle(title);
        reservation.setMember(member);
        reservation.setPickupBranch(pickup);
        reservation.setStatus(status);
        reservation.setPlacedAt(placed);
        reservation.setReminderSent(false);
        reservation.setCancelledByLibrary(false);
        return reservations.save(reservation);
    }

    private void reserveWithFee(Member member, Title title, Branch pickup, LocalDate date) {
        Reservation reservation = reservation(title, member, pickup, ReservationStatus.WAITING, placedAt(date, 11));
        charge(member, ChargeType.RESERVATION_FEE, "0.50", "0.50", date, null, reservation, "Reservation fee");
    }

    /** Creates a charge; when the outstanding amount is lower than the original, the difference was paid in cash. */
    private Charge charge(Member member, ChargeType type, String original, String outstanding, LocalDate date,
                          Loan loan, Reservation reservation, String description) {
        BigDecimal originalAmount = new BigDecimal(original);
        BigDecimal outstandingAmount = new BigDecimal(outstanding);
        Charge charge = new Charge();
        charge.setMember(member);
        charge.setType(type);
        charge.setOriginalAmount(originalAmount);
        charge.setOutstandingAmount(outstandingAmount);
        charge.setChargeDate(date);
        charge.setStatus(outstandingAmount.signum() > 0 ? ChargeStatus.OPEN : ChargeStatus.SETTLED);
        charge.setLoan(loan);
        charge.setReservation(reservation);
        charge.setDescription(description);
        charges.save(charge);

        BigDecimal paid = originalAmount.subtract(outstandingAmount);
        if (paid.signum() > 0) {
            Payment payment = new Payment();
            payment.setMember(member);
            payment.setAmount(paid);
            payment.setMethod(PaymentMethod.CASH);
            payment.setPaymentDate(date.plusDays(1));
            payments.save(payment);

            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setPayment(payment);
            allocation.setCharge(charge);
            allocation.setAmount(paid);
            allocations.save(allocation);

            ChargeTransaction tx = new ChargeTransaction();
            tx.setCharge(charge);
            tx.setType(TransactionType.PAYMENT);
            tx.setAmount(paid);
            tx.setTransactionDate(date.plusDays(1));
            tx.setNote("Cash payment");
            chargeTransactions.save(tx);
        }
        return charge;
    }

    private Instant placedAt(LocalDate date, int hour) {
        return date.atTime(hour, 0).atZone(ZoneId.of(properties.timeZone())).toInstant();
    }

    private static LocalDate date(String iso) {
        return LocalDate.parse(iso);
    }
}
