package be.npl.shelfwise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.config.RulesProperties;
import be.npl.shelfwise.finance.helper.FineCalculator;
import be.npl.shelfwise.finance.helper.FineCalculator.FineResult;
import be.npl.shelfwise.member.entity.MemberCategory;
import be.npl.shelfwise.support.Holidays2026;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Appendix A.3: the examples of the overdue fine (F-FIN-01 to F-FIN-06). */
class FineCalculatorTest {

    private static final RulesProperties RULES = RulesProperties.defaults();

    /** The daily rate before any discount: the ADULT rate for ADULT and SENIOR, the YOUTH rate for YOUTH. */
    private static BigDecimal rate(MemberCategory category, MaterialType material) {
        if (category == MemberCategory.YOUTH) {
            return material == MaterialType.AUDIOVISUAL ? new BigDecimal("0.50") : BigDecimal.ZERO;
        }
        return switch (material) {
            case BOOK -> new BigDecimal("0.25");
            case AUDIOVISUAL -> new BigDecimal("1.00");
            case MAGAZINE -> new BigDecimal("0.10");
        };
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("A.3 overdue fines")
    @CsvSource({
            "AC-FIN-01, ADULT,  BOOK,        25.00, 2026-03-02, 2026-03-03, 1,  0.00",
            "AC-FIN-02, ADULT,  BOOK,        25.00, 2026-03-02, 2026-03-04, 2,  0.50",
            "AC-FIN-03, ADULT,  BOOK,        25.00, 2026-03-06, 2026-03-13, 6,  1.50",
            "AC-FIN-04, ADULT,  BOOK,        25.00, 2026-04-04, 2026-04-08, 2,  0.50",
            "AC-FIN-05, SENIOR, BOOK,        25.00, 2026-03-02, 2026-03-07, 5,  0.63",
            "AC-FIN-06, ADULT,  AUDIOVISUAL,  8.00, 2026-03-02, 2026-03-16, 12, 8.00",
            "AC-FIN-07, ADULT,  AUDIOVISUAL, 20.00, 2026-03-02, 2026-03-16, 12, 10.00",
            "AC-FIN-08, SENIOR, AUDIOVISUAL, 20.00, 2026-03-02, 2026-03-05, 3,  1.50",
            "AC-FIN-09, SENIOR, AUDIOVISUAL, 20.00, 2026-03-02, 2026-03-31, 25, 10.00",
            "AC-FIN-10, YOUTH,  BOOK,        25.00, 2026-03-02, 2026-03-16, 12, 0.00",
            "AC-FIN-11, YOUTH,  AUDIOVISUAL, 20.00, 2026-03-02, 2026-03-04, 2,  1.00",
            "AC-FIN-12, ADULT,  MAGAZINE,     6.00, 2026-03-02, 2026-03-07, 5,  0.50",
            "AC-FIN-13, ADULT,  BOOK,        25.00, 2026-03-02, 2026-03-04, 2,  0.50",
    })
    void overdueFine(String id, MemberCategory category, MaterialType material, String replacementCost, LocalDate due,
                     LocalDate returned, int chargeableDays, String fine) {
        FineResult result = FineCalculator.calculate(due, returned, category, rate(category, material),
                new BigDecimal(replacementCost), Holidays2026.CLOSING, RULES);
        assertThat(result.chargeableDays()).as(id + " chargeable days").isEqualTo(chargeableDays);
        assertThat(result.fine()).as(id + " fine").isEqualByComparingTo(fine);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("F-FIN-01: no fine when returned on or before the due date")
    @CsvSource({"2026-03-02, 2026-03-02", "2026-03-02, 2026-02-27"})
    void noFineOnTime(LocalDate due, LocalDate returned) {
        FineResult result = FineCalculator.calculate(due, returned, MemberCategory.ADULT, new BigDecimal("0.25"),
                new BigDecimal("25.00"), Holidays2026.CLOSING, RULES);
        assertThat(result.chargeableDays()).isZero();
        assertThat(result.fine()).isEqualByComparingTo("0.00");
    }
}
