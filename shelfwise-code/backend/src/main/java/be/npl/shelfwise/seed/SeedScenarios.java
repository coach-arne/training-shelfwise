package be.npl.shelfwise.seed;

import java.util.List;

/** The members of Appendix B.3, in the order in which they are seeded (S-n has card number NPL00000n). */
public final class SeedScenarios {

    public record Scenario(String code, String firstName, String lastName, String purpose) {
    }

    public static final List<Scenario> MEMBERS = List.of(
            new Scenario("S-01", "Ada", "Adams", "Happy path: no loans, no charges"),
            new Scenario("S-02", "Ben", "Brooks", "Loan limit: 10 open loans (AC-MEM-05)"),
            new Scenario("S-03", "Cleo", "Carter", "YOUTH loan limit, guardian notifications (AC-MEM-06)"),
            new Scenario("S-04", "Dan", "Carter", "Guardian of S-03 and S-13"),
            new Scenario("S-05", "Eva", "Evans", "Blocked at exactly 5.00 (AC-MEM-02/03)"),
            new Scenario("S-06", "Finn", "Foster", "Not blocked at 4.99 (AC-MEM-01)"),
            new Scenario("S-07", "Gina", "Gray", "Expired membership (AC-MEM-04)"),
            new Scenario("S-08", "Hugo", "Hale", "SENIOR with accrued fine 1.50"),
            new Scenario("S-09", "Iris", "Irwin", "Declared lost on 2026-03-17"),
            new Scenario("S-10", "Jack", "Jones", "Maximum renewals reached"),
            new Scenario("S-11", "Kim", "King", "Renewal refused by a reservation (AC-LEN-08)"),
            new Scenario("S-12", "Leo", "Lane", "5 waiting reservations (AC-RES-03)"),
            new Scenario("S-13", "Mia", "Moore", "YOUTH with accrued fines: BOOK 0.00, AUDIOVISUAL 6.00"),
            new Scenario("S-14", "Noah", "Nash", "Keeps reading history (AC-MEM-09)"),
            new Scenario("S-15", "Olga", "Owen", "Anonymisation (AC-MEM-08, AC-MEM-10)"),
            new Scenario("S-16", "Pia", "Price", "Hold expires on 2026-03-16"),
            new Scenario("S-17", "Quinn", "Quill", "Next in the queue after Pia"),
            new Scenario("S-18", "Rosa", "Reed", "Payment allocation (AC-PAY-01)"),
            new Scenario("S-19", "Sam", "Stone", "Same person as librarian.cen (AC-FIN-22)"));

    private SeedScenarios() {
    }
}
